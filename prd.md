# Product Requirements — AI Call Assistant

We are building our own AI Call Assistant inspired by the publicly observable features of Equal AI. Our app must have its own implementation, UI, branding, and backend.

Project path:
`/Users/sayyadmazaharmehadi/aicallassistant`

## 1. Core product goal

When the user cannot answer a call, an AI assistant should be able to handle the conversation, understand why the person is calling, capture important details, and present a useful record to the user afterward.

We will build this in phases. The first version must work without real cellular-call interception.

## 2. Required features

### A. Authentication and user account
- Sign up, sign in, and sign out using Supabase Auth.
- Support email/password authentication initially.
- Keep each user's calls, transcripts, summaries, settings, and audio private.
- Allow users to manage their profile and sign out securely.

### B. Home dashboard
- Show recent calls, missed/simulated call records, and call summaries.
- Provide a prominent **Simulate Call** action for testing.
- Display useful call information: caller name or number when available, date, duration, status, and summary.
- Include navigation to call history, settings, and individual call details.

### C. AI voice assistant
- Use Gemini Live through the FastAPI backend.
- Stream microphone audio from the Android device to the backend and stream the AI's audio responses back.
- Support natural, two-way voice conversation with low latency.
- Introduce itself as an AI assistant when appropriate.
- Ask who is calling, why they are calling, how urgent the matter is, and whether a message should be passed on.
- Respond naturally to follow-up questions and clarify unclear information.
- Never invent information about the user or promise actions it cannot perform.
- Handle silence, interruptions where supported, connection errors, and session termination gracefully.

### D. Call history and details
- Save a record for every completed simulated call.
- Show a chronological list of calls with searchable caller names and messages.
- Provide a detail screen containing the transcript, summary, duration, timestamp, and call status.
- Allow the user to delete individual call records and associated audio according to the retention policy.

### E. Automatic summaries and important information
After each conversation, generate or derive:
- A concise summary.
- The caller's identity and reason for calling, when stated.
- Important facts, dates, times, and phone numbers mentioned.
- Urgency level with a clear explanation.
- Action items or requested follow-ups.
- Uncertainties where the caller did not provide enough information.

Do not treat AI-generated guesses as confirmed facts.

### F. Recordings and transcripts
- Store transcripts linked to the relevant call.
- Support private audio recording storage where technically and legally permitted.
- Provide audio playback when an audio recording exists.
- Show recording availability separately from transcript availability.
- Ask for and respect required permissions and consent; provide clear recording indicators and retention controls.
- Do not record secretly or claim a recording exists when it does not.

### G. Important calls and action items
- Highlight urgent calls and important messages.
- Let users mark a call as important.
- Let users mark action items as pending or completed.
- Provide a simple way to copy a summary or message.
- Keep reminders and external actions out of scope until explicitly implemented and authorized.

### H. Settings
- Configure the AI assistant's name and greeting.
- Configure its conversation style and preferred language where supported.
- Configure recording and retention preferences.
- Show account and privacy information.
- Provide a way to delete stored call data and sign out.

## 3. Technology stack

- Android app: Kotlin + Jetpack Compose.
- Backend: Python + FastAPI.
- Real-time voice: Gemini Live API, accessed only by the backend.
- Authentication: Supabase Auth.
- Database: Supabase PostgreSQL.
- Audio storage: private Supabase Storage bucket.
- Development: Android phone connects to the Mac-hosted backend over local Wi-Fi.
- Production: secure HTTPS/WSS and authenticated backend requests.

Never put Gemini API keys or Supabase service-role keys in the Android app. Use backend environment variables and keep secrets out of Git.

## 4. Important limitation: real cellular calls

The first milestone supports simulated calls initiated from our app. It must not claim to answer arbitrary incoming SIM calls.

Real missed-call answering is a later milestone that may require a compatible telephony provider, carrier call forwarding, and country-specific support. Evaluate providers for India before promising this feature.

Keep the architecture modular so a future telephony provider can feed audio into the same conversation and post-call processing pipeline.

## 5. Architecture and quality requirements

- Keep the Android client, backend, and database responsibilities separate.
- Use authenticated ownership checks and Supabase Row Level Security.
- Validate all backend inputs and handle expired sessions.
- Use secure WebSocket session handling and sensible audio size limits.
- Save a call's transcript and summary reliably even when audio storage fails.
- Handle interrupted connections and duplicate save requests.
- Include loading, empty, error, permission-denied, and offline states.
- Avoid unnecessary packages and paid services beyond the required Gemini API usage.
- Keep the UI modern, polished, accessible, and easy for a non-technical user.

## 6. Build milestones

1. Finalize architecture and API contracts.
2. Create the project structure, environment configuration, and Supabase schema/security rules.
3. Build authentication and the dashboard.
4. Implement the simulated-call voice pipeline.
5. Implement call history, transcripts, summaries, and action items.
6. Add private audio storage, playback, and retention controls.
7. Test security, failure cases, Android permissions, and end-to-end behavior.
8. Research and plan real cellular-call integration separately.

## 7. Instructions for the current task

Compare these requirements with the existing research and architecture work. Identify missing requirements, conflicts, technical risks, and features that should be deferred.

Do not start implementing the whole app in one pass. Update the architecture and implementation plan first, preserve existing work, and report the changes and remaining decisions.

Work one milestone at a time. Wait for approval before moving from planning into implementation.