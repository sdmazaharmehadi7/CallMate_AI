# Implementation Roadmap & Student Work Breakdown

## 1. Phased Implementation Roadmap

```mermaid
gantt
    title AI Call Assistant Implementation Phases
    dateFormat  YYYY-MM-DD
    section Phase 1: Foundation
    Supabase Schema & Storage Migrations       :p1_1, 2026-10-19, 2d
    FastAPI Boilerplate & Auth Middleware     :p1_2, after p1_1, 2d
    Gemini Live API WebSocket Spike           :p1_3, after p1_2, 3d
    section Phase 2: Android Core
    Android Compose Scaffolding & Design System:p2_1, 2026-10-19, 3d
    Supabase Auth & Navigation Flow           :p2_2, after p2_1, 2d
    Call History & Action Items Screens       :p2_3, after p2_2, 2d
    section Phase 3: Live Audio
    AudioRecord (16kHz) & AudioTrack (24kHz)  :p3_1, 2026-10-26, 3d
    Bidirectional WebSocket Audio Streaming   :p3_2, after p3_1, 3d
    Interruption & Barge-in Handling          :p3_3, after p3_2, 2d
    section Phase 4: Post-Call
    Structured Summaries & Action Items Extraction:p4_1, 2026-11-03, 2d
    Audio Recording Playback with Signed URLs :p4_2, after p4_1, 2d
    Supabase Realtime Sync Polish             :p4_3, after p4_2, 2d
    section Phase 5: Hardening
    Latency Benchmarking & Jitter Tuning      :p5_1, 2026-11-10, 3d
    Edge Cases, Network Drops & Error States  :p5_2, after p5_1, 2d
```

---

## 2. Two-Person Student Work Breakdown

To ensure parallel progress without merge conflicts, work is split cleanly across system boundaries:

### Developer 1: Backend, Voice AI & Cloud Infrastructure Lead
* **Primary Responsibilities**:
  1. **Supabase Schema & Storage**: Create database tables (`profiles`, `calls`, `call_transcripts`, `action_items`), RLS policies, and private `call-recordings` storage bucket.
  2. **FastAPI Web Service**: Setup async project with FastAPI, Pydantic models, and Supabase JWT authentication middleware.
  3. **Gemini Live Gateway**: Implement bidirectional WebSocket bridge connecting to Google GenAI Live API (`wss://generativelanguage.googleapis.com`).
  4. **Audio Transcoding & WAV Packaging**: Collect incoming and outgoing audio frames in memory and compile valid WAV audio files for Supabase Storage.
  5. **Post-Call Structured LLM Pipeline**: Trigger Gemini Flash structured JSON extraction for 2-sentence summary, caller intent, urgency flag, and action items upon call hangup.

### Developer 2: Android Client, UI/UX & Native Audio Lead
* **Primary Responsibilities**:
  1. **Jetpack Compose Architecture**: Scaffold Android project with Material 3, Navigation Compose, and clean MVVM ViewModel structure.
  2. **Supabase Mobile Client**: Integrate `supabase-kt` for email/password and OTP authentication, profile loading, and call history fetching.
  3. **Hardware Audio Capture & Playback**:
     - Implement `AudioRecord` engine (PCM 16kHz, 16-bit mono, 100ms frames) using `VOICE_COMMUNICATION` source for hardware echo cancellation.
     - Implement `AudioTrack` engine (PCM 24kHz, 16-bit mono) with instant flush capability on barge-in.
  4. **Call Simulation & Live Transcript UI**:
     - Build dynamic simulated call screen with active audio visualizer, turn-by-turn auto-scrolling transcript, and "End Call" button.
     - Handle `RECORD_AUDIO` runtime permission flow gracefully.
  5. **Call Detail & Playback Screen**: Waveform audio player consuming signed URLs, summary card, and interactive action items checklist.

---

## 3. Step-by-Step Development Commands

### 3.1 Backend Setup (Developer 1 on Mac)
```bash
# 1. Navigate to backend directory
mkdir -p aicallassistant-backend && cd aicallassistant-backend

# 2. Create Python virtual environment (Python 3.13)
python3 -m venv venv
source venv/bin/activate

# 3. Install core dependencies
pip install --upgrade pip
pip install fastapi uvicorn[standard] google-genai supabase websockets pydantic pydantic-settings python-dotenv numpy

# 4. Freeze dependencies
pip freeze > requirements.txt

# 5. Run development server (accessible over local Wi-Fi)
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 3.2 Android Setup (Developer 2 on Mac / Android Studio)
```bash
# 1. Project Directory: aicallassistant-android
# 2. Minimum SDK: Android 8.0 (API 26) | Target SDK: Android 15 (API 35)
# 3. Gradle build verification:
./gradlew assembleDebug

# 4. Port forward to allow Android device to reach local Mac server over USB (optional alternative to Wi-Fi)
~/Library/Android/sdk/platform-tools/adb reverse tcp:8000 tcp:8000
```

---

## 4. Unresolved Technical Questions & Spike Checklist

Before writing full production code, the following targeted experimental spikes will be run:

| Spike Target | Key Question to Resolve | Success Metric |
| :--- | :--- | :--- |
| **Spike 1: Gemini Live Latency** | What is the round-trip latency from audio chunk transmission to first audio byte response? | First response byte < **800 ms** over local Wi-Fi. |
| **Spike 2: Barge-in Audio Flush** | Does `AudioTrack.flush()` completely stop audio playback instantly without crackle or buffer overflow? | Audio cut-off within **< 150 ms** of user speech. |
| **Spike 3: Hardware Echo Cancellation** | Does `AudioSource.VOICE_COMMUNICATION` eliminate speaker-to-mic feedback loop during simultaneous playback? | Zero echo leakage into Gemini Live stream. |
| **Spike 4: WebSocket Keepalive** | Does the local Wi-Fi WebSocket maintain connection stability during 3-5 minute conversations? | Zero unexpected disconnects or dropped frames. |

---

## 5. Next Immediate Action: Phase 1 Scaffolding

Upon your approval, we will proceed to execute **Phase 1**:
1. Apply the Supabase SQL schema and Storage bucket setup using our active Supabase project (`iacddfhvbippjjtsdtyq`).
2. Scaffold the `aicallassistant-backend` directory with FastAPI, environment templates, and the Gemini Live API audio streaming gateway.
3. Scaffold the `aicallassistant-android` Jetpack Compose project skeleton.
