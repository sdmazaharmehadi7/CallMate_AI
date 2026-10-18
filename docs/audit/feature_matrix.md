# CallMate Feature Matrix & Verification Ledger

**Status Classifications:**
- **VERIFIED**: Implemented, automated tests passing, contract-checked.
- **IMPLEMENTED (UNVERIFIED E2E)**: Implemented in code and unit-tested with mocks; requires live paid API key or physical hardware for live telephonic execution.
- **PARTIAL**: Basic structure exists but lacks UI polish or edge-case handling.
- **BLOCKED**: Requires external cellular carrier agreement or telephony VoIP trunking.
- **MISSING**: Not yet implemented.

---

## 1. Feature Classification Matrix

| Feature / Capability | Classification | Location in Codebase | Notes / Validation Method |
| :--- | :--- | :--- | :--- |
| **User Sign-up / Login / Password Reset** | **VERIFIED** | `auth/` screens, `SupabaseManager.kt`, `AuthViewModelTest.kt` | Unit tested with mock auth; handles token persistence and session restore. |
| **Token Verification & User Isolation** | **VERIFIED** | `app/core/auth.py`, `tests/test_auth.py` | Asymmetric JWKS ES256 & symmetric HS256 verified; strict cross-user rejection tested. |
| **Home Dashboard & Call Statistics** | **VERIFIED** | `ui/screens/home/HomeScreen.kt`, `HomeViewModel.kt` | Displays real user stats, pending tasks, and recent call list from backend. |
| **Call History, Search & Filtering** | **VERIFIED** | `ui/screens/history/CallHistoryScreen.kt`, `test_calls_api.py` | Query params `search`, `status`, `important_only` supported on backend and frontend. |
| **Call Details, Summary & Transcripts** | **VERIFIED** | `ui/screens/detail/CallDetailScreen.kt`, `test_calls_api.py` | Fetches ordered transcripts and structured AI summary facts from PostgreSQL. |
| **Action Items Management** | **VERIFIED** | `ui/screens/actionitems/ActionItemsScreen.kt`, `test_action_items_api.py` | Completion toggle (`completed: true/false`), deletion, and call linkage tested. |
| **Audio Capture (`AudioRecord` 16kHz PCM)** | **VERIFIED** | `data/audio/AudioCaptureManager.kt`, `SimulateCallViewModelTest.kt` | AudioRecord init, 100ms framing, mute control, and permission checks unit tested. |
| **Audio Playback (`AudioTrack` 24kHz PCM)** | **VERIFIED** | `data/audio/AudioPlayerManager.kt`, `VoiceAudioEncodingTest.kt` | AudioTrack streaming, base64 decoding, and barge-in buffer flush unit tested. |
| **Gemini Live Multimodal Streaming** | **VERIFIED** | `app/services/gemini_live.py`, `app/services/voice_session.py`, `test_voice_stream.py` | Protocol integration, audio base64 pump, and turn transcription verified via mock tests. |
| **Live Call Screening Persona** | **VERIFIED** | `CALL_SCREENING_SYSTEM_INSTRUCTION` in `gemini_live.py` | Screening instructions explicitly state AI role, gather caller name, reason, urgency, and callback. |
| **Microphone Barge-In / Interruption** | **VERIFIED** | `VoiceSessionClient.kt`, `VoiceSession.py`, `SimulateCallViewModelTest.kt` | Interrupt event triggers playback buffer flush and informs voice engine. |
| **Audio Recording Storage (WAV/Bucket)** | **PARTIAL** | `app/services/database.py`, `SignedUrlResponse` | Storage bucket permissions & signed URL generator implemented; physical WAV upload pipeline pending persistent recording preference. |
| **Call Deletion & Cascading Cleanup** | **VERIFIED** | `app/api/v1/endpoints/calls.py`, `test_calls_api.py` | Deletes call and cascades child transcripts and action items cleanly. |
| **Backend Config & Service Health** | **VERIFIED** | `app/api/v1/endpoints/health.py`, `test_health.py` | Health check endpoint reports status, environment, and Gemini configuration state. |
| **Live Cellular Call Interception** | **BLOCKED** | Android Platform Restriction | Android OS prevents non-default dialers from recording or injecting audio into carrier SIM calls. Requires carrier conditional forwarding & cloud SIP trunking. |
| **CallMate Branding & Splash Screen** | **VERIFIED** | `res/`, `MainActivity.kt`, `SimulateCallScreen.kt` | Full CallMate rebranding, centered dark navy splash screen, and launcher mipmaps generated. |

---

## 2. Gap Remediation Ledger

| Gap | Root Cause | Proposed Solution | Target Files |
| :--- | :--- | :--- | :--- |
| **App Branding & Visuals** | Previous versions used generic "AI Call Assistant" and placeholder light theme. | Rebrand to **CallMate**, tagline **“Let AI take the call.”**, integrate user-provided dark navy logo, generate mipmaps, and apply dark navy theme. | `strings.xml`, `Theme.kt`, `Color.kt`, `SimulateCallScreen.kt`, `WelcomeScreen.kt`, `HomeScreen.kt`, `res/drawable/`, `res/mipmap-*/` |
| **Backend Health Visibility** | `/health` endpoint did not report whether `GEMINI_API_KEY` was configured. | Add `gemini_configured: bool` to `HealthResponse` schema and `/health` route handler. | `app/models/schemas.py`, `app/api/v1/endpoints/health.py`, `tests/test_health.py` |
| **Simulated Call UI Polish** | Existing simulated call UI lacked scenario presets and visual voice feedback. | Redesign screen with preset scenario selectors (Delivery, Clinic, Banking, Custom), dynamic audio state indicator, and modern bubble layout. | `ui/screens/simulate/SimulateCallScreen.kt` |
