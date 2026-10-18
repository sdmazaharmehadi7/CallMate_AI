# AI Call Assistant - FastAPI Backend

FastAPI backend orchestrating simulated AI call answering, voice streaming, and call records management.

## Requirements
- Python 3.11+ (Tested on Python 3.13)
- macOS (or Linux)

---

## Local Setup on macOS

### 1. Create and Activate Virtual Environment
```bash
cd aicallassistant-backend
python3 -m venv venv
source venv/bin/activate
```

### 2. Install Dependencies
```bash
pip install --upgrade pip
pip install -r requirements.txt
```

### 3. Configure Environment Variables
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
*Note: Do not commit `.env` to version control. Keep real API keys out of client builds.*

### 4. Run the Development Server
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```
The server will start on:
- Local machine: `http://localhost:8000`
- Interactive API Docs: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/health`
- Authenticated user endpoint: `http://localhost:8000/api/v1/auth/me`

---

## Authentication & Supabase JWT Verification Design

The backend verifies user identity on every protected endpoint using standard Supabase Auth tokens:

1. **Modern Asymmetric JWKS (ES256 / RS256 - Default)**:
   - Modern Supabase Auth signs JWTs using asymmetric cryptography (e.g., `ES256` Elliptic Curve P-256).
   - The backend validates tokens using public keys retrieved and cached from the project's JWKS endpoint:
     `{SUPABASE_URL}/auth/v1/.well-known/jwks.json`
   - **No shared JWT secret is needed on the backend** for asymmetric verification.
2. **Optional Symmetric HS256 Support**:
   - For legacy Supabase projects configured with HMAC-SHA256 (`HS256`), setting `SUPABASE_JWT_SECRET` in `.env` enables symmetric HMAC verification.
   - If a client supplies an `HS256` token but `SUPABASE_JWT_SECRET` is unset, the backend explicitly rejects the request with an actionable error.
3. **Verified Claims**:
   - Signature: verified against matching public key (JWKS) or secret (HMAC).
   - Expiration (`exp`): strictly enforced with 10-second clock-skew leeway.
   - Audience (`aud`): verified to be `"authenticated"`.
   - Issuer (`iss`): verified to match `{SUPABASE_URL}/auth/v1`.
   - Subject (`sub`): extracted as the authenticated user's unique UUID.

### Credential Boundaries
- **Android App**: Receives only `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` (or anon key). It sends the user's ephemeral session JWT in the `Authorization: Bearer <token>` header.
- **Backend**: Holds `GEMINI_API_KEY` and `SUPABASE_SERVICE_ROLE_KEY`. Neither credential is ever shared with the mobile client.

---

## Required Environment Variables

| Variable | Description | Required By | Security Level |
| :--- | :--- | :--- | :--- |
| `HOST` | Bind host (default: `0.0.0.0`) | Server | Internal |
| `PORT` | Bind port (default: `8000`) | Server | Internal |
| `ENVIRONMENT` | Environment name (`development` / `production`) | Server | Internal |
| `ALLOWED_ORIGINS` | Comma-separated CORS allowed origins | FastAPI CORS | Internal |
| `SUPABASE_URL` | Full Supabase project URL | Auth & DB | Public |
| `SUPABASE_PUBLISHABLE_KEY` | Client publishable / anon key | Client / Auth | Public |
| `SUPABASE_SERVICE_ROLE_KEY` | Administrative service role key | Backend DB Admin | **Secret (Backend only)** |
| `SUPABASE_JWT_SECRET` | *(Optional)* Shared secret for legacy HS256 projects | Auth (Legacy HS256) | **Secret (Backend only)** |
| `GEMINI_API_KEY` | Google Gemini API key for Live audio | Backend Voice AI | **Secret (Backend only)** |
| `GEMINI_MODEL` | Gemini Multimodal Live model (default: `gemini-2.0-flash-exp`) | Backend Voice AI | Internal |
| `GEMINI_DEFAULT_VOICE`| Default voice persona (e.g. `Puck`, `Charon`, `Aoede`) | Backend Voice AI | Internal |
| `GEMINI_LIVE_CONNECTION_TIMEOUT_SECONDS` | Timeout connecting to Gemini Live (default: `15`) | Backend Voice AI | Internal |
| `GEMINI_SESSION_MAX_DURATION_SECONDS` | Maximum allowed duration for a call stream (default: `900` / 15m) | Backend Voice AI | Internal |
| `GEMINI_MAX_AUDIO_CHUNK_BYTES` | Maximum incoming audio chunk size (default: `65536` / 64KB) | Backend Voice AI | Internal |

---

## Real-Time Voice Streaming WebSocket API

The backend integrates with Google's **Gemini Multimodal Live API** via the `google-genai` SDK for low-latency bidirectional voice screening.

### WebSocket Endpoint
- **URL**: `ws://<backend-host>:8000/ws/calls/{call_id}/stream?token=<supabase_access_token>`
- **Alias**: `ws://<backend-host>:8000/api/v1/calls/{call_id}/stream?token=<supabase_access_token>`
- **Authentication**: Valid Supabase session access token provided as a URL query parameter (`token=...`). Verified against Supabase JWKS / JWT secrets before any session begins.
- **Ownership**: The backend verifies that `calls.user_id` matches the authenticated token's subject (`sub`). Unauthenticated or unauthorized connection attempts receive an error frame and close immediately with code `4401` or `4403`.
- **Concurrency**: Active calls are tracked in memory; duplicate concurrent sessions for the same call are rejected with close code `4409` (`SESSION_CONFLICT`).

### Audio Specifications
- **Client Input Audio (`audio_in`)**:
  - Format: Raw linear PCM, 16-bit signed integer, Little-endian, Mono.
  - Sample Rate: **16,000 Hz (16 kHz)**.
  - Recommended Chunk: 100 ms frames (3,200 bytes raw before base64 encoding).
  - Maximum Chunk Size: 64 KB (`GEMINI_MAX_AUDIO_CHUNK_BYTES`).
- **AI Output Audio (`audio_out`)**:
  - Format: Raw linear PCM, 16-bit signed integer, Little-endian, Mono.
  - Sample Rate: **24,000 Hz (24 kHz)**.
  - Encoding: Base64 encoded in JSON frames.

### WebSocket Events Protocol

#### Client -> Server (Android to FastAPI):
1. **`audio_in`**:
   ```json
   {
     "event": "audio_in",
     "data": "<base64_encoded_pcm_16000_mono>",
     "timestamp_ms": 1420
   }
   ```
2. **`interrupt`**: Notifies server that caller started speaking (barge-in):
   ```json
   {
     "event": "interrupt",
     "timestamp_ms": 2840
   }
   ```
3. **`end_call`**: Gracefully terminates the call session:
   ```json
   {
     "event": "end_call",
     "reason": "user_hangup"
   }
   ```

#### Server -> Client (FastAPI to Android):
1. **`call_started`**: Handshake confirmed and Gemini Live session active:
   ```json
   {
     "event": "call_started",
     "call_id": "c7a8b6e2-...",
     "model": "gemini-2.0-flash-exp",
     "voice": "Puck"
   }
   ```
2. **`audio_out`**: Synthesized speech chunk for immediate playback on Android `AudioTrack`:
   ```json
   {
     "event": "audio_out",
     "data": "<base64_encoded_pcm_24000_mono>",
     "timestamp_ms": 1820
   }
   ```
3. **`transcript_delta`**: Real-time transcribed text turn:
   ```json
   {
     "event": "transcript_delta",
     "speaker": "assistant",
     "text": "Hello, I am an AI assistant...",
     "is_final": false,
     "timestamp_ms": 1900
   }
   ```
4. **`interrupted`**: Barge-in signal detected by Gemini Live; Android client clears audio buffer:
   ```json
   {
     "event": "interrupted"
   }
   ```
5. **`call_ended`**: Session concluded and summary persisted:
   ```json
   {
     "event": "call_ended",
     "call_id": "c7a8b6e2-...",
     "summary": "Courier arrived with delivery.",
     "duration_seconds": 45
   }
   ```
6. **`error`**: Operational or validation error:
   ```json
   {
     "event": "error",
     "code": "AUDIO_CHUNK_TOO_LARGE | INVALID_JSON | GEMINI_CONNECTION_FAILED | ...",
     "message": "Descriptive message"
   }
   ```

---

## Connecting from Android Device or Emulator

### Android Studio Emulator
The emulator uses the loopback alias:
```
http://10.0.2.2:8000
ws://10.0.2.2:8000
```

### Physical Android Device (over USB via ADB reverse)
If your Android phone is connected via USB with USB Debugging enabled:
```bash
~/Library/Android/sdk/platform-tools/adb reverse tcp:8000 tcp:8000
```
Then the app connects to `http://localhost:8000` and `ws://localhost:8000`.

### Physical Android Device (over Local Wi-Fi)
Ensure both the Mac and Android phone are on the same Wi-Fi network. Find your Mac's IP (e.g. `10.165.44.67`) and configure the app:
```
http://10.165.44.67:8000
ws://10.165.44.67:8000
```

---

## Running Automated Tests

Activate the virtual environment and run `pytest`:
```bash
source venv/bin/activate
pytest -v
```
All tests run with mocked/generated keys in memory; no live remote calls or credentials are required. Test coverage includes:
- JWT authentication, JWKS verification, and call ownership checks.
- Missing Gemini configuration handling without server startup crash.
- Bidirectional WebSocket streaming handshake and audio frames.
- Interruption handling, audio size limits, and malformed frames.
- Active session registry duplicate prevention.
- Post-call database persistence for ordered transcripts, summaries, and action items.
