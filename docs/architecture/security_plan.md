# Security, Privacy & Zero-Trust Architecture Plan

## 1. Threat Model & Zero-Trust Secrets Management

### 1.1 Credential Boundaries & Secret Isolation
Under no circumstances are administrative or API vendor secrets stored in or transmitted to the Android mobile application.

```mermaid
graph LR
    subgraph Untrusted_Client_Zone["Untrusted Client Zone (Android Device)"]
        APK[Android APK]
        AnonKey[SUPABASE_ANON_KEY (Public)]
        UserJWT[Ephemeral User Session JWT]
    end

    subgraph Trusted_Server_Zone["Trusted Server Zone (FastAPI on Mac / Cloud)"]
        EnvFile[Backend .env (Git-Ignored)]
        GeminiKey[GEMINI_API_KEY]
        SupaServiceKey[SUPABASE_SERVICE_ROLE_KEY]
        FastAPIServer[FastAPI Application]
    end

    subgraph External_Clouds["External Cloud Providers"]
        GeminiCloud[Google Gemini Live API]
        SupabaseCloud[Supabase Auth & Database]
    end

    APK -->|User JWT Only| FastAPIServer
    FastAPIServer -->|GEMINI_API_KEY| GeminiCloud
    FastAPIServer -->|SUPABASE_SERVICE_ROLE_KEY| SupabaseCloud
    APK -->|Anon Key + JWT Only| SupabaseCloud
```

| Credential | Permitted Location | Prohibited Location | Exposure Impact |
| :--- | :--- | :--- | :--- |
| **`GEMINI_API_KEY`** | FastAPI server `.env` only | Android client, Git repos, build logs | High: Unauthorized AI quota consumption |
| **`SUPABASE_SERVICE_ROLE_KEY`** | FastAPI server `.env` only | Android client, Git repos | Critical: Full bypass of all database RLS |
| **`SUPABASE_ANON_KEY`** | Android `BuildConfig` / `.env` | None (Public by design) | None: Operations bounded by PostgreSQL RLS |
| **`User Access Token (JWT)`** | Android Secure Keystore / memory | Permanent plain-text files | Medium: Session hijacking until expiry |

### 1.2 APK Reverse-Engineering Defense
If an attacker decompiles the Android APK (using JADX, Apktool, or REA):
1. **Zero Secret Leakage**: No API keys, database credentials, or secret endpoints exist in the APK bytecode.
2. **Obfuscation**: Production builds enable Android R8/ProGuard code shrinking and obfuscation.
3. **Network Security Config**: Strict TLS domain pinning in production; local cleartext HTTP permitted only for local development IP (`10.165.44.67` and `10.0.2.2`).

---

## 2. Supabase Row Level Security (RLS) Policies

All database tables enforce PostgreSQL Row Level Security. Even with a valid user JWT, a user cannot query, modify, or delete another user's call logs, transcripts, or action items.

```sql
-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.calls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.call_transcripts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.action_items ENABLE ROW LEVEL SECURITY;

-- 1. Profiles Policies
CREATE POLICY "Users can view their own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- 2. Calls Policies
CREATE POLICY "Users can view their own calls"
    ON public.calls FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own calls"
    ON public.calls FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own calls"
    ON public.calls FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own calls"
    ON public.calls FOR DELETE
    USING (auth.uid() = user_id);

-- 3. Call Transcripts Policies (Access scoped via parent call ownership)
CREATE POLICY "Users can view transcripts of their own calls"
    ON public.call_transcripts FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = call_transcripts.call_id
            AND calls.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert transcripts for their own calls"
    ON public.call_transcripts FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.calls
            WHERE calls.id = call_transcripts.call_id
            AND calls.user_id = auth.uid()
        )
    );

-- 4. Action Items Policies
CREATE POLICY "Users can view their own action items"
    ON public.action_items FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can update their own action items"
    ON public.action_items FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own action items"
    ON public.action_items FOR DELETE
    USING (auth.uid() = user_id);
```

---

## 3. Storage Security (Private Audio Recordings)

### 3.1 Bucket Configuration
* Bucket name: `call-recordings`
* Public access: **`false`** (Completely private; direct HTTP access disallowed).
* File path convention: `{user_id}/{call_id}.wav`

### 3.2 Storage Access Policies (PostgreSQL `storage.objects`)
```sql
-- Allow users to read only their own audio recordings
CREATE POLICY "Users can read own audio recordings"
    ON storage.objects FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'call-recordings' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );

-- Allow backend service role or authenticated owner to upload
CREATE POLICY "Users can upload own audio recordings"
    ON storage.objects FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'call-recordings' AND
        (storage.foldername(name))[1] = auth.uid()::text
    );
```

### 3.3 Ephemeral Playback Authorization
When the Android user plays a recorded call, the mobile app requests a signed URL (`GET /api/v1/calls/{call_id}/recording-url`). The backend verifies user ownership and generates a cryptographic HMAC signed URL with a **15-minute expiration**. Raw audio is never made public.

---

## 4. Audio Privacy, Device Permissions & Data Retention

### 4.1 Android Permissions Lifecycle
* **`android.permission.RECORD_AUDIO`**:
  - Dynamically requested at runtime immediately prior to launching the call simulation.
  - If denied, the app displays a clear rationale dialog explaining why microphone access is required for voice conversation.
  - If permanently denied, provides a direct link to the Android App Settings page.
* **Ephemeral In-Memory Buffers**:
  - Audio frames captured by `AudioRecord` are streamed immediately to the WebSocket buffer in RAM.
  - Raw PCM audio is **never** written to unencrypted Android temporary storage or external cache directories.
  - Memory buffers are explicitly zeroed and recycled upon call termination.

### 4.2 Legal Compliance & Caller Consent (Indian DPDP Act & Global Standards)
1. **Automated Identification Notice**: Every call greeting clearly announces the AI assistant identity ("Hello, this is Mazahar's AI assistant...").
2. **Recording Consent Notice**: The assistant greeting states: "This call is recorded for notes and summary."
3. **Data Erasure**: When a user deletes a call from the Android app, a cascading delete deletes the database call record, transcripts, action items, and the audio recording file from Supabase Storage.

---

## 5. Environment Variables & Secret Configuration Template

### Backend (`.env` in `aicallassistant-backend/`)
```ini
# Server Configuration
HOST=0.0.0.0
PORT=8000
ENVIRONMENT=development
ALLOWED_ORIGINS=http://localhost:3000,http://10.0.2.2:8000,http://10.165.44.67:8000

# Google Gemini API
GEMINI_API_KEY=AIzaSy...your-gemini-api-key-here
GEMINI_MODEL=gemini-2.0-flash-exp
GEMINI_VOICE=Puck

# Supabase Admin Configuration
SUPABASE_URL=https://iacddfhvbippjjtsdtyq.supabase.co
SUPABASE_SERVICE_ROLE_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
SUPABASE_JWT_SECRET=your-supabase-jwt-secret
SUPABASE_STORAGE_BUCKET=call-recordings
```

### Android (`local.properties` in `aicallassistant-android/`)
```properties
## DO NOT COMMIT TO VERSION CONTROL
supabase.url=https://iacddfhvbippjjtsdtyq.supabase.co
supabase.anon.key=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
backend.base.url=http://10.165.44.67:8000
backend.ws.url=ws://10.165.44.67:8000
```
