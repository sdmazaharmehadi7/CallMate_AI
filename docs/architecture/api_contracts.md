# API Contracts, WebSocket Protocols & Database Schemas

## 1. REST API Specification (FastAPI)

All endpoints require standard Bearer JWT authentication in the `Authorization` header (`Bearer <supabase_access_token>`), unless explicitly noted.

### 1.1 `POST /api/v1/calls/simulate`
Initiates a new simulated call session. Creates a pending call record in Supabase.

* **Request Headers**:
  ```http
  Authorization: Bearer <supabase_access_token>
  Content-Type: application/json
  ```
* **Request Body**:
  ```json
  {
    "caller_name": "Delivery Agent (Simulated)",
    "caller_number": "+919876543210",
    "voice_persona": "Puck",
    "language": "en-IN"
  }
  ```
* **Response `201 Created`**:
  ```json
  {
    "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
    "status": "connecting",
    "ws_url": "ws://0.0.0.0:8000/ws/calls/c7a8b6e2-1234-4567-89ab-cdef01234567/stream",
    "created_at": "2026-10-18T15:30:00.000Z"
  }
  ```

---

### 1.2 `GET /api/v1/calls`
Retrieves paginated call history for the authenticated user (newest first).

* **Query Parameters**:
  - `limit` (int, default: 20, min: 1, max: 100)
  - `offset` (int, default: 0, min: 0)
  - `status` (string, optional: `connecting`, `active`, `completed`, `failed`, `cancelled`)
  - `search` (string, optional: searches caller name/number, reason, and summary)
  - `important_only` (bool, optional: filter calls where `is_important = true`)
* **Response `200 OK`**:
  ```json
  {
    "calls": [
      {
        "id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
        "caller_name": "Delivery Agent (Simulated)",
        "caller_number": "+919876543210",
        "status": "completed",
        "call_type": "simulated",
        "caller_reason": "Package delivery",
        "summary": "Courier arrived with Amazon package. Instructed to leave at neighbor.",
        "urgency": "low",
        "is_important": false,
        "duration_seconds": 45,
        "has_recording": true,
        "created_at": "2026-10-18T15:30:00.000Z"
      }
    ],
    "total": 1,
    "limit": 20,
    "offset": 0
  }
  ```

---

### 1.3 `GET /api/v1/calls/{call_id}`
Retrieves full details for a single call owned by the authenticated user, including turn transcripts and action items.

* **Response `200 OK`**:
  ```json
  {
    "id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
    "caller_name": "Delivery Agent (Simulated)",
    "caller_number": "+919876543210",
    "status": "completed",
    "call_type": "simulated",
    "caller_reason": "Package delivery",
    "summary": "Courier arrived with Amazon package. Instructed to leave at neighbor.",
    "urgency": "low",
    "urgency_explanation": "Routine delivery completed without issues",
    "uncertainties": null,
    "key_facts": ["Package delivered to neighbor at flat 402"],
    "is_important": false,
    "duration_seconds": 45,
    "recording_path": "user-uuid/c7a8b6e2-1234-4567-89ab-cdef01234567.wav",
    "recording_duration_seconds": 45,
    "recording_size_bytes": 1440000,
    "has_recording": true,
    "created_at": "2026-10-18T15:30:00.000Z",
    "started_at": "2026-10-18T15:30:05.000Z",
    "ended_at": "2026-10-18T15:30:50.000Z",
    "transcripts": [
      {
        "id": "t1-1234",
        "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
        "speaker": "assistant",
        "text": "Hello, this is an AI assistant screening a call. How can I help you?",
        "timestamp_ms": 1200,
        "sequence_order": 0,
        "created_at": "2026-10-18T15:30:06.000Z"
      }
    ],
    "action_items": [
      {
        "id": "a1-9012",
        "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
        "task": "Collect parcel from neighbor at flat 402",
        "due_date": null,
        "completed": false,
        "completed_at": null,
        "created_at": "2026-10-18T15:30:51.000Z"
      }
    ]
  }
  ```

---

### 1.4 `PATCH /api/v1/calls/{call_id}`
Updates mutable call fields (`is_important`, `status`, `caller_reason`, `summary`, `urgency`, `urgency_explanation`, `uncertainties`, `key_facts`, `duration_seconds`).
Enforces lifecycle status transitions (`connecting` -> `active` -> `completed`/`failed`/`cancelled`).

* **Request Body**:
  ```json
  {
    "is_important": true,
    "status": "completed"
  }
  ```
* **Response `200 OK`**: Updated `CallDetailResponse`.

---

### 1.5 `DELETE /api/v1/calls/{call_id}`
Deletes an owned call record, automatically cascading child transcripts and action items in PostgreSQL, and safely deleting any associated storage file from the private `call-recordings` bucket.

* **Response `200 OK`**:
  ```json
  {
    "deleted": true,
    "id": "c7a8b6e2-1234-4567-89ab-cdef01234567"
  }
  ```

---

### 1.6 `GET /api/v1/calls/{call_id}/transcripts`
Retrieves ordered turn-by-turn transcripts for an owned call.

* **Response `200 OK`**:
  ```json
  {
    "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
    "transcripts": [
      {
        "id": "t1-1234",
        "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
        "speaker": "assistant",
        "text": "Hello, how can I help you?",
        "timestamp_ms": 100,
        "sequence_order": 0,
        "created_at": "2026-10-18T15:30:06.000Z"
      }
    ],
    "total": 1
  }
  ```

---

### 1.7 `POST /api/v1/calls/{call_id}/transcripts`
Appends a dialog turn entry to an owned call. Enforces duplicate prevention and idempotent retries on `(call_id, sequence_order)`.

* **Request Body**:
  ```json
  {
    "speaker": "caller",
    "text": "I have arrived at your building gate.",
    "timestamp_ms": 3200,
    "sequence_order": 1
  }
  ```
* **Response `201 Created`**: Returns `TranscriptItem`. (If retried with identical speaker and text, returns existing entry).

---

### 1.8 `GET /api/v1/calls/{call_id}/summary`
Retrieves stored AI summary facts, urgency, and uncertainties. Returns `404 Not Found` if no completed summary exists yet (never fabricates data).

* **Response `200 OK`**:
  ```json
  {
    "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
    "summary": "Courier arrived with Amazon package.",
    "caller_reason": "Delivery verification",
    "urgency": "low",
    "urgency_explanation": "Routine package delivery",
    "key_facts": ["Package delivered to flat 402"],
    "uncertainties": null,
    "duration_seconds": 45
  }
  ```

---

### 1.9 `PUT /api/v1/calls/{call_id}/summary`
Persists or updates the AI-generated call summary, urgency level, and extracted facts.

* **Request Body**:
  ```json
  {
    "summary": "Courier arrived with Amazon package.",
    "caller_reason": "Delivery verification",
    "urgency": "low",
    "urgency_explanation": "Routine package delivery",
    "key_facts": ["Package delivered to flat 402"],
    "uncertainties": null
  }
  ```
* **Response `200 OK`**: Returns `CallSummaryResponse`.

---

### 1.10 `GET /api/v1/calls/{call_id}/action-items`
Lists all follow-up action items created from an owned call.

* **Response `200 OK`**:
  ```json
  {
    "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
    "action_items": [
      {
        "id": "a1-9012",
        "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
        "task": "Collect parcel from neighbor at flat 402",
        "due_date": null,
        "completed": false,
        "completed_at": null,
        "created_at": "2026-10-18T15:30:51.000Z"
      }
    ],
    "total": 1
  }
  ```

---

### 1.11 `POST /api/v1/calls/{call_id}/action-items`
Creates a follow-up action item linked to an owned call.

* **Request Body**:
  ```json
  {
    "task": "Collect parcel from neighbor at flat 402",
    "due_date": null
  }
  ```
* **Response `201 Created`**: Returns created `ActionItem`.

---

### 1.12 `PATCH /api/v1/action-items/{action_item_id}`
Updates an action item's completion status, task description, or due date. Verifies ownership through its parent call. Setting `completed: true` automatically populates `completed_at` with the current UTC timestamp; setting `completed: false` clears `completed_at`.

* **Request Body**:
  ```json
  {
    "completed": true
  }
  ```
* **Response `200 OK`**: Returns updated `ActionItem`.

---

### 1.13 `DELETE /api/v1/action-items/{action_item_id}`
Deletes an action item after verifying ownership through its parent call.

* **Response `200 OK`**:
  ```json
  {
    "deleted": true,
    "id": "a1-9012"
  }
  ```

---

### 1.14 `GET /api/v1/calls/{call_id}/recording-url`
Generates a short-lived (15 minutes) signed URL from Supabase Storage for secure audio playback.
Strictly verifies that:
1. The authenticated caller owns the parent call.
2. A non-null `recording_path` exists on the record.
3. The storage path strictly adheres to the user isolation boundary (`{user_id}/{call_id}.wav`).
4. The file exists in the private `call-recordings` bucket.

* **Response `200 OK`**:
  ```json
  {
    "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
    "signed_url": "https://iacddfhvbippjjtsdtyq.supabase.co/storage/v1/object/sign/call-recordings/user-id/call-id.wav?token=...",
    "expires_at": "2026-10-18T15:45:00.000Z",
    "recording_path": "user-id/call-id.wav",
    "file_size_bytes": 1440000
  }
  ```

---

### 1.15 Error Responses
All endpoints return consistent JSON error envelopes:
```json
{
  "error": {
    "code": "NOT_FOUND | UNAUTHORIZED | FORBIDDEN | BAD_REQUEST | CONFLICT | VALIDATION_ERROR | INTERNAL_ERROR",
    "message": "Human-readable error description without internal SQL or secret leakage",
    "details": null
  }
}
```

---

## 2. WebSocket Real-Time Streaming Protocol

* **URL**: `/ws/calls/{call_id}/stream?token=<supabase_access_token>`
* **Format**: All WebSocket messages are JSON objects containing an `event` discriminator field.

### 2.1 Client to Server Frames (Android -> FastAPI)

#### `audio_in`
Transfers a chunk of microphone audio recorded from the user.
```json
{
  "event": "audio_in",
  "data": "<base64_encoded_pcm_16000_mono_little_endian>",
  "timestamp_ms": 1420
}
```
*Audio specifications*: Raw linear PCM, 16 kHz sample rate, 16-bit signed integer, Mono. Each frame is 100ms (3,200 bytes raw before base64).

#### `interrupt`
Informs the server that the user started speaking, requesting an immediate stop to AI speech playback.
```json
{
  "event": "interrupt",
  "timestamp_ms": 2840
}
```

#### `end_call`
Terminates the active call session gracefully.
```json
{
  "event": "end_call",
  "reason": "user_hangup"
}
```

---

### 2.2 Server to Client Frames (FastAPI -> Android)

#### `call_started`
Acknowledges successful handshake with Gemini Live API.
```json
{
  "event": "call_started",
  "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
  "model": "gemini-2.0-flash-exp",
  "voice": "Puck"
}
```

#### `audio_out`
Delivers synthesized speech chunks from Gemini Live for immediate playback through the Android speaker.
```json
{
  "event": "audio_out",
  "data": "<base64_encoded_pcm_24000_mono_little_endian>",
  "timestamp_ms": 1820
}
```
*Audio specifications*: Raw linear PCM, 24 kHz sample rate, 16-bit signed integer, Mono.

#### `transcript_delta`
Streams turn-by-turn text chunks in real-time as the user or assistant speaks.
```json
{
  "event": "transcript_delta",
  "speaker": "assistant",
  "text": "Hello, how can I help you?",
  "is_final": false,
  "timestamp_ms": 1900
}
```

#### `interrupted`
Notifies Android that Gemini Live has detected user barge-in. Android immediately clears its `AudioTrack` buffer.
```json
{
  "event": "interrupted"
}
```

#### `call_ended`
Notifies Android that the call has terminated and post-processing has completed.
```json
{
  "event": "call_ended",
  "call_id": "c7a8b6e2-1234-4567-89ab-cdef01234567",
  "summary": "Courier arrived with Amazon package.",
  "duration_seconds": 45
}
```

#### `error`
Reports an operational error.
```json
{
  "event": "error",
  "code": "GEMINI_DISCONNECTED",
  "message": "Voice AI session closed unexpectedly. Reconnecting..."
}
```

---

## 3. Supabase PostgreSQL Database Schema

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Profiles Table (Extends Supabase auth.users)
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT,
    phone_number TEXT,
    assistant_voice TEXT DEFAULT 'Puck',
    assistant_persona TEXT DEFAULT 'friendly',
    language TEXT DEFAULT 'en-IN',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Calls Table
CREATE TABLE public.calls (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    caller_name TEXT DEFAULT 'Unknown',
    caller_number TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('connecting', 'active', 'completed', 'failed')),
    call_type TEXT NOT NULL DEFAULT 'simulated' CHECK (call_type IN ('simulated', 'cellular_forwarded')),
    duration_seconds INTEGER DEFAULT 0,
    intent_category TEXT,
    summary TEXT,
    urgent BOOLEAN DEFAULT FALSE,
    recording_path TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    ended_at TIMESTAMPTZ
);

-- Index for fast user call history queries
CREATE INDEX idx_calls_user_id_created_at ON public.calls(user_id, created_at DESC);

-- 3. Call Transcripts Table
CREATE TABLE public.call_transcripts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    call_id UUID NOT NULL REFERENCES public.calls(id) ON DELETE CASCADE,
    speaker TEXT NOT NULL CHECK (speaker IN ('caller', 'assistant')),
    text TEXT NOT NULL,
    timestamp_ms INTEGER NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_transcripts_call_id ON public.call_transcripts(call_id, timestamp_ms ASC);

-- 4. Action Items Table
CREATE TABLE public.action_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    call_id UUID NOT NULL REFERENCES public.calls(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    task TEXT NOT NULL,
    urgency TEXT DEFAULT 'medium' CHECK (urgency IN ('low', 'medium', 'high')),
    completed BOOLEAN DEFAULT FALSE,
    due_date TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_action_items_user_id ON public.action_items(user_id, completed);
```

---

## 4. Kotlin & Python Data Transfer Objects (DTOs)

### Kotlin DTOs (Android)
```kotlin
@Serializable
data class SimulateCallRequest(
    val caller_name: String = "Delivery Agent (Simulated)",
    val caller_number: String = "+919876543210",
    val voice_persona: String = "Puck",
    val language: String = "en-IN"
)

@Serializable
data class SimulateCallResponse(
    val call_id: String,
    val status: String,
    val ws_url: String,
    val created_at: String
)

@Serializable
sealed interface CallWebSocketFrame {
    @Serializable
    @SerialName("audio_in")
    data class AudioIn(val data: String, val timestamp_ms: Long) : CallWebSocketFrame

    @Serializable
    @SerialName("audio_out")
    data class AudioOut(val data: String, val timestamp_ms: Long) : CallWebSocketFrame

    @Serializable
    @SerialName("transcript_delta")
    data class TranscriptDelta(val speaker: String, val text: String, val is_final: Boolean) : CallWebSocketFrame

    @Serializable
    @SerialName("interrupted")
    data object Interrupted : CallWebSocketFrame

    @Serializable
    @SerialName("end_call")
    data class EndCall(val reason: String = "user_hangup") : CallWebSocketFrame
}
```

### Python Pydantic Models (FastAPI)
```python
from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime

class SimulateCallRequest(BaseModel):
    caller_name: str = "Delivery Agent (Simulated)"
    caller_number: str = "+919876543210"
    voice_persona: str = "Puck"
    language: str = "en-IN"

class SimulateCallResponse(BaseModel):
    call_id: str
    status: str
    ws_url: str
    created_at: datetime

class CallSummaryResponse(BaseModel):
    call_id: str
    summary: str
    intent_category: str
    urgent: bool
    action_items: List[str]
```
