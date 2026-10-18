-- =============================================================================
-- Migration: 20261018160800_initial_schema.sql
-- Description: Core schema, indexes, RLS policies, and storage setup for AI Call Assistant
-- Security: Strict Row Level Security enforced on all tables; storage bucket is private.
-- =============================================================================

-- Ensure cryptographic UUID generation is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- 1. PROFILES TABLE
-- One profile per authenticated user in auth.users.
-- Stores user identity, assistant personalization, and privacy/retention settings.
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT,
    phone_number TEXT,
    assistant_name TEXT NOT NULL DEFAULT 'AI Assistant',
    assistant_greeting TEXT NOT NULL DEFAULT 'Hello, this is an AI assistant screening a call on behalf of the user. How can I help you today?',
    assistant_voice TEXT NOT NULL DEFAULT 'Puck',
    assistant_persona TEXT NOT NULL DEFAULT 'friendly_concise',
    preferred_language TEXT NOT NULL DEFAULT 'en-IN',
    recording_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    retention_days INTEGER NOT NULL DEFAULT 30 CHECK (retention_days > 0 AND retention_days <= 365),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE public.profiles IS 'User settings, personalized assistant persona, and retention preferences.';

-- Trigger to auto-create a profile row when a new user signs up in auth.users
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.profiles (id, full_name, phone_number)
    VALUES (
        new.id,
        COALESCE(new.raw_user_meta_data->>'full_name', ''),
        COALESCE(new.phone, new.raw_user_meta_data->>'phone_number', '')
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Updated_at timestamp trigger for profiles
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS trigger AS $$
BEGIN
    new.updated_at = now();
    RETURN new;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS on_profile_updated ON public.profiles;
CREATE TRIGGER on_profile_updated
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();


-- =============================================================================
-- 2. CALLS TABLE
-- Master call records representing each simulated (or future carrier-forwarded) call.
-- Stores caller identity, timing, AI-derived summary, urgency, and recording metadata.
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.calls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    caller_name TEXT,
    caller_number TEXT NOT NULL DEFAULT 'Unknown',
    status TEXT NOT NULL DEFAULT 'connecting' CHECK (
        status IN ('connecting', 'active', 'completed', 'failed', 'cancelled')
    ),
    call_type TEXT NOT NULL DEFAULT 'simulated' CHECK (
        call_type IN ('simulated', 'cellular_forwarded')
    ),
    caller_reason TEXT,
    summary TEXT,
    urgency TEXT NOT NULL DEFAULT 'low' CHECK (
        urgency IN ('low', 'medium', 'high', 'critical')
    ),
    urgency_explanation TEXT,
    uncertainties TEXT,
    key_facts JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_important BOOLEAN NOT NULL DEFAULT FALSE,
    duration_seconds INTEGER NOT NULL DEFAULT 0 CHECK (duration_seconds >= 0),
    recording_path TEXT,
    recording_duration_seconds INTEGER CHECK (recording_duration_seconds IS NULL OR recording_duration_seconds >= 0),
    recording_size_bytes BIGINT CHECK (recording_size_bytes IS NULL OR recording_size_bytes >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ
);

COMMENT ON TABLE public.calls IS 'Master record for every call handled by the AI assistant.';

-- Query optimization indexes
CREATE INDEX IF NOT EXISTS idx_calls_user_created_at
    ON public.calls (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_calls_user_important
    ON public.calls (user_id, is_important)
    WHERE is_important = TRUE;

CREATE INDEX IF NOT EXISTS idx_calls_user_status
    ON public.calls (user_id, status);


-- =============================================================================
-- 3. CALL TRANSCRIPTS TABLE
-- Turn-by-turn text dialog captured during the live call conversation.
-- Strict ordering guaranteed by sequence_order.
-- Ownership is derived strictly from parent call (calls.user_id).
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.call_transcripts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_id UUID NOT NULL REFERENCES public.calls(id) ON DELETE CASCADE,
    speaker TEXT NOT NULL CHECK (speaker IN ('caller', 'assistant')),
    text TEXT NOT NULL,
    timestamp_ms INTEGER NOT NULL DEFAULT 0 CHECK (timestamp_ms >= 0),
    sequence_order INTEGER NOT NULL CHECK (sequence_order >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_call_transcript_sequence UNIQUE (call_id, sequence_order)
);

COMMENT ON TABLE public.call_transcripts IS 'Turn-by-turn dialog transcripts linked to a specific call.';

CREATE INDEX IF NOT EXISTS idx_transcripts_call_sequence
    ON public.call_transcripts (call_id, sequence_order ASC);


-- =============================================================================
-- 4. ACTION ITEMS TABLE
-- Tasks and requested follow-ups extracted from the conversation.
-- Ownership is derived strictly from parent call (calls.user_id).
-- =============================================================================

CREATE TABLE IF NOT EXISTS public.action_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_id UUID NOT NULL REFERENCES public.calls(id) ON DELETE CASCADE,
    task TEXT NOT NULL,
    due_date TIMESTAMPTZ,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE public.action_items IS 'Actionable follow-ups derived from call conversations.';

CREATE INDEX IF NOT EXISTS idx_action_items_call_id
    ON public.action_items (call_id);

CREATE INDEX IF NOT EXISTS idx_action_items_completed
    ON public.action_items (call_id, completed);


-- =============================================================================
-- 5. ROW LEVEL SECURITY (RLS) POLICIES
-- Strict isolation: users can only access records associated with their auth.uid().
-- Transcripts and action items enforce ownership via parent call joins.
-- =============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.call_transcripts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.action_items ENABLE ROW LEVEL SECURITY;

-- -----------------------------------------------------------------------------
-- 5.1 Profiles RLS
-- -----------------------------------------------------------------------------
DROP POLICY IF EXISTS "profiles_select_own" ON public.profiles;
CREATE POLICY "profiles_select_own"
    ON public.profiles FOR SELECT
    TO authenticated
    USING (auth.uid() = id);

DROP POLICY IF EXISTS "profiles_insert_own" ON public.profiles;
CREATE POLICY "profiles_insert_own"
    ON public.profiles FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "profiles_update_own" ON public.profiles;
CREATE POLICY "profiles_update_own"
    ON public.profiles FOR UPDATE
    TO authenticated
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "profiles_delete_own" ON public.profiles;
CREATE POLICY "profiles_delete_own"
    ON public.profiles FOR DELETE
    TO authenticated
    USING (auth.uid() = id);


-- -----------------------------------------------------------------------------
-- 5.2 Calls RLS
-- -----------------------------------------------------------------------------
DROP POLICY IF EXISTS "calls_select_own" ON public.calls;
CREATE POLICY "calls_select_own"
    ON public.calls FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "calls_insert_own" ON public.calls;
CREATE POLICY "calls_insert_own"
    ON public.calls FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "calls_update_own" ON public.calls;
CREATE POLICY "calls_update_own"
    ON public.calls FOR UPDATE
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "calls_delete_own" ON public.calls;
CREATE POLICY "calls_delete_own"
    ON public.calls FOR DELETE
    TO authenticated
    USING (auth.uid() = user_id);


-- -----------------------------------------------------------------------------
-- 5.3 Call Transcripts RLS (Ownership verified via parent calls table)
-- -----------------------------------------------------------------------------
DROP POLICY IF EXISTS "transcripts_select_parent_call" ON public.call_transcripts;
CREATE POLICY "transcripts_select_parent_call"
    ON public.call_transcripts FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = call_transcripts.call_id
            AND calls.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "transcripts_insert_parent_call" ON public.call_transcripts;
CREATE POLICY "transcripts_insert_parent_call"
    ON public.call_transcripts FOR INSERT
    TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = call_transcripts.call_id
            AND calls.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "transcripts_update_parent_call" ON public.call_transcripts;
CREATE POLICY "transcripts_update_parent_call"
    ON public.call_transcripts FOR UPDATE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = call_transcripts.call_id
            AND calls.user_id = auth.uid()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = call_transcripts.call_id
            AND calls.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "transcripts_delete_parent_call" ON public.call_transcripts;
CREATE POLICY "transcripts_delete_parent_call"
    ON public.call_transcripts FOR DELETE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = call_transcripts.call_id
            AND calls.user_id = auth.uid()
        )
    );


-- -----------------------------------------------------------------------------
-- 5.4 Action Items RLS (Ownership verified via parent calls table)
-- -----------------------------------------------------------------------------
DROP POLICY IF EXISTS "action_items_select_parent_call" ON public.action_items;
CREATE POLICY "action_items_select_parent_call"
    ON public.action_items FOR SELECT
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = action_items.call_id
            AND calls.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "action_items_insert_parent_call" ON public.action_items;
CREATE POLICY "action_items_insert_parent_call"
    ON public.action_items FOR INSERT
    TO authenticated
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = action_items.call_id
            AND calls.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "action_items_update_parent_call" ON public.action_items;
CREATE POLICY "action_items_update_parent_call"
    ON public.action_items FOR UPDATE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = action_items.call_id
            AND calls.user_id = auth.uid()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = action_items.call_id
            AND calls.user_id = auth.uid()
        )
    );

DROP POLICY IF EXISTS "action_items_delete_parent_call" ON public.action_items;
CREATE POLICY "action_items_delete_parent_call"
    ON public.action_items FOR DELETE
    TO authenticated
    USING (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = action_items.call_id
            AND calls.user_id = auth.uid()
        )
    );


-- =============================================================================
-- 6. PRIVATE STORAGE BUCKET: call-recordings
-- Strictly private (public = false) S3-compatible audio storage.
-- Files are organized by user UUID: "{user_id}/{call_id}.wav".
-- Policies verify that the top-level path segment matches auth.uid().
-- =============================================================================

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'call-recordings',
    'call-recordings',
    FALSE,
    52428800, -- 50 MB max per audio recording
    ARRAY['audio/wav', 'audio/x-wav', 'audio/mpeg', 'audio/mp4']
)
ON CONFLICT (id) DO UPDATE SET
    public = FALSE,
    file_size_limit = 52428800,
    allowed_mime_types = ARRAY['audio/wav', 'audio/x-wav', 'audio/mpeg', 'audio/mp4'];

-- Storage RLS Policies
DROP POLICY IF EXISTS "recordings_select_own" ON storage.objects;
CREATE POLICY "recordings_select_own"
    ON storage.objects FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'call-recordings' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "recordings_insert_own" ON storage.objects;
CREATE POLICY "recordings_insert_own"
    ON storage.objects FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'call-recordings' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "recordings_update_own" ON storage.objects;
CREATE POLICY "recordings_update_own"
    ON storage.objects FOR UPDATE
    TO authenticated
    USING (
        bucket_id = 'call-recordings' AND
        (storage.foldername(name))[1] = auth.uid()::text
    )
    WITH CHECK (
        bucket_id = 'call-recordings' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );

DROP POLICY IF EXISTS "recordings_delete_own" ON storage.objects;
CREATE POLICY "recordings_delete_own"
    ON storage.objects FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'call-recordings' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );
