# Implementation Plan: CallMate Modernization & Finalization

**Goal:** Execute full rebranding to **CallMate** with tagline **“Let AI take the call.”**, modernize Android UI with the dark navy theme and user-provided logo splash screen, enhance backend health reporting, maintain 100% test passing rate, and produce a signed debug APK.

---

## Stage 1: Backend Health & Configuration Enhancements
1. Add `gemini_configured: bool` to `HealthResponse` schema in `app/models/schemas.py`.
2. Update `/health` endpoint in `app/api/v1/endpoints/health.py` to return `settings.is_gemini_configured()`.
3. Update `tests/test_health.py` to assert `gemini_configured`.
4. Run `source venv/bin/activate && pytest` and confirm 56+ tests passing.

---

## Stage 2: CallMate Branding & Asset Integration
1. Update `res/values/strings.xml`:
   - `app_name` = `"CallMate"`
   - `tagline` = `"Let AI take the call."`
2. Integrate user-provided logo (`CallMate AI Voice Logo.png`):
   - Already generated: `res/drawable/callmate_logo.png`, `res/drawable-nodpi/callmate_logo_highres.png`.
   - Already generated launcher icons: `res/mipmap-mdpi/`, `res/mipmap-hdpi/`, `res/mipmap-xhdpi/`, `res/mipmap-xxhdpi/`, `res/mipmap-xxxhdpi/`.
3. Configure Splash Screen:
   - Create `SplashScreen.kt` with a dark navy background (`#080B27`), centered CallMate logo, smooth fade-in animation, and seamless navigation to `Main` or `Welcome`.
   - Update `res/values/themes.xml` with window background `#080B27`.

---

## Stage 3: Modern Dark Navy Theme & Design System
1. Update `ui/theme/Color.kt`:
   - Dark Navy Backgrounds: `#080B27` (deep canvas), `#0D1126` (surface), `#141A36` (surface elevated).
   - Brand Accents: CallMate Cyan/Blue gradient (`#00BCD4` / `#4FACFE` / `#00E5FF`).
   - Clean text colors: `#FFFFFF` (high emphasis), `#A0ABC0` (medium emphasis).
   - Urgency pills: Low (emerald green), Medium (amber), High (coral), Critical (crimson).
2. Update `ui/theme/Theme.kt`:
   - Enforce CallMate Dark Navy color scheme as default for a cohesive, premium experience.

---

## Stage 4: Screen-by-Screen Modernization
1. **WelcomeScreen & AuthScreens (`Login`, `SignUp`, `ForgotPassword`)**:
   - Prominently display CallMate logo, title, and tagline **"Let AI take the call."**
   - Clean dark navy cards with responsive text fields and primary gradient buttons.
2. **HomeScreen**:
   - Header with CallMate branding and user identity.
   - Quick "Simulate AI Voice Call" banner with cyan gradient glow.
   - Real summary metrics (Total Calls, Important Calls, Pending Tasks).
   - Recent calls list with urgency pills.
3. **SimulateCallScreen (Phase D)**:
   - Voice-first redesign with caller scenario selector chips:
     - 🚚 *Courier Delivery*
     - 🩺 *Doctor Appointment*
     - 🏦 *Bank Verification*
     - ✏️ *Custom Caller*
   - Pulsing dynamic state pill: `Connecting`, `Listening`, `AI Speaking`, `Muted`, `Reconnecting`.
   - Auto-scrolling transcript conversation bubbles with speaker chips.
   - Accessible bottom controls: Mute toggle, Barge-in button, prominent End Call button.
   - Post-call summary card with urgency chip and key facts.
4. **CallHistoryScreen**:
   - Search bar with instant text filtering.
   - Filter chips: All, Important, Urgent, Completed.
   - Clean card items with formatted duration and timestamps.
5. **CallDetailScreen**:
   - Modern layout dividing Caller Info, AI Summary & Urgency, Turn-by-Turn Transcript, Action Items, and Recording Status.
6. **ActionItemsScreen**:
   - Tabs for Pending vs Completed tasks.
   - One-tap checkbox completion.
7. **SettingsScreen**:
   - User account details, Assistant Persona preferences, Backend connection test & URL editor, Sign out.

---

## Stage 5: Verification & Packaging
1. Run `./gradlew testDebugUnitTest` and verify all tests pass.
2. Run `./gradlew assembleDebug` and generate debug APK.
3. Verify APK signature with `apksigner`.
4. Run backend `pytest` and verify 100% pass rate.
5. Copy updated APK to `CallMate.apk` and `aicallassistant.apk`.
6. Provide comprehensive final report with manual testing steps.
