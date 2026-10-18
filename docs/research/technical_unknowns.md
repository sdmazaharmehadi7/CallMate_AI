# Technical Unknowns, Architecture Trade-Offs, and Risk Mitigation Plan

This document identifies the critical architectural unknowns, hardware/network dependencies, latency bottlenecks, and compliance factors involved in building an AI call assistant on Android with our target stack:
- **Android**: Kotlin + Jetpack Compose
- **Backend**: Python + FastAPI
- **Voice AI**: Gemini Live API (Bidirectional real-time voice & streaming WebSocket)
- **Database / Auth / Storage**: Supabase (PostgreSQL, Supabase Auth, Realtime, Storage)
- **Development**: Local Mac
- **Production**: Hosted Cloud Service

---

## 1. Technical Unknowns & Engineering Investigations

### Unknown 1: Telephony Ingestion & Conditional Call Forwarding Mechanics
* **Description**: How does an incoming cellular phone call reach our FastAPI backend and Gemini Live API when the user doesn't answer or declines the call?
* **Analysis**:
  - Pure Android apps *cannot* programmatically intercept cellular voice audio directly off an active cellular voice call without being the default system dialer or telecom connection service, and even then, Android's security sandbox prevents background audio injection into a cellular voice call on most modern Android versions (Android 10+).
  - **The Industry Standard Approach (as used by Equal AI, Truecaller Assistant, Google Call Screen on Pixel carrier setups)**: Uses telecom **Conditional Call Forwarding** (CCF). When the phone is busy (`*67*`), unanswered (`*61*`), or unreachable (`*62*`), the cellular carrier forwards the incoming call via PSTN/SIP to a designated virtual phone number (DID) hosted on a cloud telephony provider (e.g., Twilio, Exotel, Plivo, Agora, or a self-hosted FreeSWITCH/Kamailio SIP server).
* **Open Questions**:
  1. Do all Indian carriers (Jio, Airtel, Vodafone Idea, BSNL) and international carriers support USSD-based conditional call forwarding uniformly (e.g., `*004*<VirtualNumber>#` or `*61*`) across prepaid and postpaid plans?
  2. How does the cloud telephony provider identify which user the forwarded call was originally intended for? (Answer: Via SIP `Diversion` or `History-Info` headers, or by provisioning dedicated/pooled virtual numbers).
* **Mitigation / Spike**:
  - Build a minimal telephony spike with Twilio/Exotel webhook receiving a forwarded call and verifying the `To`, `From`, and `ForwardedFrom` (Diversion) headers.

---

### Unknown 2: Audio Bridging Between Telephony (PSTN/SIP) and Gemini Live API
* **Description**: Telephony audio streams typically operate over G.711 (PCMU/PCMA 8kHz, 8-bit mono) or G.722, whereas Gemini Live API requires linear PCM 16kHz or 24kHz (16-bit, little-endian, mono) over a secure bidirectional WebSocket.
* **Analysis**:
  - The FastAPI backend must act as an ultra-low-latency real-time audio bridge:
    1. Receive inbound media stream packets over WebSocket from the telephony provider (e.g., Twilio Media Streams, Exotel Voice Stream).
    2. Resample 8kHz PCM/mu-law to 16kHz/24kHz linear PCM.
    3. Stream audio chunks via WebSocket to Gemini Live API (`models/gemini-2.0-flash-exp` or Gemini Live endpoint).
    4. Receive Gemini Live audio output (PCM 24kHz), downsample to 8kHz mu-law, and send back to the telephony WebSocket.
  - End-to-end voice latency must be strictly under **800ms - 1200ms** to avoid awkward conversational pauses.
* **Mitigation / Spike**:
  - Develop a standalone Python FastAPI service using `asyncio` and `numpy`/`soxr`/`audioop` for real-time audio transcoding and measure round-trip audio latency against Gemini Live API.

---

### Unknown 3: Live Call Takeover Mechanics
* **Description**: When the user sees an important call being screened and taps "Take Over" in the Android app, how is the user connected into the active call?
* **Analysis**:
  - **Approach A (In-App WebRTC VoIP)**: The Android app initiates a WebRTC voice session directly with the backend. The backend conferences the user's WebRTC audio with the caller's PSTN leg, and mutes or disconnects the Gemini AI leg.
    - *Pros*: Seamless, instantaneous takeover within the app UI without cellular carrier call transfer charges.
    - *Cons*: Requires active mobile data / Wi-Fi with good network stability.
  - **Approach B (Outbound Cellular Dial-back / PSTN Bridge)**: The telephony gateway dials the user's cellular number, and once answered, conferences all three legs together before dropping the AI assistant.
    - *Pros*: Bulletproof audio quality regardless of mobile data connectivity.
    - *Cons*: Slight 2-4 second delay while the user's phone rings.
* **Mitigation / Spike**:
  - Start with Approach A (WebRTC audio bridge using LiveKit or Twilio Voice SDK in Jetpack Compose) with fallback to Approach B.

---

### Unknown 4: Real-Time State Synchronization & Transcript Streaming
* **Description**: How does the Android client receive live transcripts and call state updates with zero noticeable lag?
* **Analysis**:
  - **Option 1 (Supabase Realtime Broadcast)**: FastAPI publishes transcript deltas to a Supabase Realtime channel (`call_transcripts:<call_id>`). The Android app subscribes using the Supabase Kotlin SDK.
    - *Pros*: Leverages existing Supabase infrastructure, zero extra server infrastructure needed, automatically authenticates via Supabase Auth JWT.
    - *Cons*: Message throughput and small broadcast latency overhead.
  - **Option 2 (Direct WebSocket to FastAPI)**: Android app opens a direct WebSocket connection to FastAPI for the duration of the active call.
    - *Pros*: Minimal possible latency (<20ms local, <100ms remote).
    - *Cons*: Requires maintaining stateful connections on the FastAPI cluster behind a load balancer.
* **Recommendation**: Use Supabase Realtime for call state & transcripts, falling back to direct WebSocket if broadcast latency exceeds 200ms.

---

### Unknown 5: Android Background Lifecycle, FCM Notifications & Call Alerting
* **Description**: When a forwarded call arrives at the backend, how is the user alerted immediately on their Android device if the app is in the background or killed?
* **Analysis**:
  - High-priority Firebase Cloud Messaging (FCM) push notification with a custom heads-up notification or incoming call notification (`NotificationCompat.Builder` with `setCategory(Notification.CATEGORY_CALL)` and full-screen intent).
  - Tapping the notification immediately opens the Jetpack Compose `LiveCallScreen`.
* **Mitigation / Spike**:
  - Configure FCM background service and test Android 13/14/15 background notification restrictions and battery optimization exemptions.

---

### Unknown 6: Legal, Regulatory & Privacy Compliance
* **Description**: Telephony voice recording and automated AI answering have legal requirements under Indian (IT Act, DPDP Act 2023, TRAI regulations) and international (FCC, GDPR, Wiretapping/Consent laws) jurisdictions.
* **Requirements**:
  1. Automated greeting must clearly state that the caller is speaking with an AI assistant on behalf of the user.
  2. If the call is recorded, an audible notice must be stated ("This call is screened and recorded for Mazahar").
  3. Strict encryption of audio recordings and transcripts stored in Supabase with Row Level Security (RLS) ensuring only the authenticated user can access their records.

---

## 2. Risk & Unknowns Summary Matrix

| Risk / Unknown | Severity | Probability | Proposed Solution / Mitigation |
| :--- | :--- | :--- | :--- |
| **Carrier Call Forwarding Inconsistency** | High | Medium | Provide interactive USSD guide per carrier (Jio, Airtel, Vi, etc.) + fallback verification check |
| **Audio Transcoding Latency (PSTN to Gemini)** | High | Low | Optimize Python `asyncio` audio stream buffer; use C-based resampling (`soxr` / `audioop-lts`) |
| **PSTN Caller Identity (Diversion Header)** | High | Low | Verify SIP `Diversion` or `X-Forwarded-For` header mapping in telephony provider |
| **Live Call Takeover Delay** | Medium | Medium | Implement WebRTC low-latency audio bridge inside Android Jetpack Compose app |
| **Background Wake-up on Inbound Call** | High | Low | Implement High-Priority FCM with `CATEGORY_CALL` full-screen intent |
| **Call Recording Privacy Regulations** | High | Low | Configurable introductory AI disclaimer ("Hi, this is Mazahar's AI assistant...") |

---

## 3. Recommended Phased Execution Strategy

1. **Phase 1: Foundation & Spikes**:
   - Initialize Git repository and project workspace.
   - Setup Supabase project (Auth, Tables with RLS: `users`, `calls`, `transcripts`, `settings`, and Storage bucket for recordings).
   - Implement FastAPI telephony webhook & Gemini Live API audio streaming spike.
2. **Phase 2: Android Jetpack Compose Core**:
   - Scaffold Android project (Kotlin, Compose, Material 3, Navigation, Supabase Kotlin SDK).
   - Implement onboarding, carrier setup UI, Call History list, Call Detail view with audio playback and structured summary.
3. **Phase 3: Live Call Screening & Real-Time Sync**:
   - Live transcript streaming screen with animated turns.
   - One-tap takeover interaction.
4. **Phase 4: Hardening & Testing**:
   - End-to-end integration test with live telephony numbers, latency measurement, and edge cases.
