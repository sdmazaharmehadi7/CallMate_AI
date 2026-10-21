import asyncio
import json
import logging
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any, AsyncGenerator, Dict, List, Optional, Union

from app.core.config import settings

logger = logging.getLogger("aicallassistant.gemini_live")

# System prompt defining the concise, natural AI call-screening persona
CALL_SCREENING_SYSTEM_INSTRUCTION = """You are CallMate, a polite, conversational voice AI assistant screening a phone call on behalf of an unavailable person.

Operating Principles for Natural Phone Speech:
1. Role & Identity: Always be upfront that you are CallMate, an AI assistant answering because the person they are trying to reach is currently unavailable.
2. Natural Phone Style: Speak naturally, warmly, and conversationally, exactly like a helpful human receptionist on a phone call. Use simple spoken language and contractions (e.g., "I'm", "they're", "don't", "can't").
3. Concise Spoken Turns: Keep almost all responses to 1 or 2 short, crisp sentences. Do NOT speak long paragraphs, lectures, or lists.
4. One Question at a Time: Ask only one relevant question per turn so the caller can easily respond without feeling overwhelmed.
5. Acknowledge and Proceed: Acknowledge what the caller said before moving to the next question (e.g., "Got it, thanks.", "Understood.", "Thanks for letting me know.").
6. Active Listening & Follow-Ups: Proactively collect the caller's name, the reason for calling, details, urgency, and callback number or message. Use prior context rather than asking for information already given.
7. Strictly Voice-Only Language: Never use markdown formatting, headings, asterisks, bullet points, numbered lists, URLs, or emojis. Output plain, spoken words only.
8. Grounded in Reality: Never invent details, appointments, or promises. Do not claim you completed an action you cannot do. If the caller asks to speak to the person, reiterate that they are unavailable and offer to take a detailed message.
9. Polite Closing: If the caller says goodbye, confirms they are done, or ends the inquiry, summarize what you noted, thank them warmly, say goodbye, and conclude the call."""


@dataclass
class LiveAudioChunk:
    data: bytes  # Raw PCM 24kHz 16-bit mono bytes


@dataclass
class LiveTranscriptChunk:
    speaker: str  # "caller" or "assistant"
    text: str
    is_final: bool = False


@dataclass
class LiveInterrupted:
    pass


@dataclass
class LiveTurnComplete:
    pass


LiveEvent = Union[LiveAudioChunk, LiveTranscriptChunk, LiveInterrupted, LiveTurnComplete]


class GeminiLiveSession(ABC):
    """Abstract interface representing an active bidirectional Gemini Live connection."""

    @abstractmethod
    async def send_audio(self, pcm_bytes: bytes) -> None:
        """Send 16kHz PCM audio chunk to Gemini Live."""
        pass

    @abstractmethod
    async def send_text(self, text: str) -> None:
        """Send a text prompt to Gemini Live."""
        pass

    @abstractmethod
    def receive_events(self) -> AsyncGenerator[LiveEvent, None]:
        """Asynchronously stream events from Gemini Live."""
        pass

    @abstractmethod
    async def close(self) -> None:
        """Close the Gemini Live session."""
        pass


class GeminiLiveService(ABC):
    """Abstract service managing Gemini Live sessions and structured post-call summaries."""

    @abstractmethod
    async def connect_session(
        self,
        voice: Optional[str] = None,
        custom_instructions: Optional[str] = None,
    ) -> GeminiLiveSession:
        """Establish a new bidirectional Live session."""
        pass

    @abstractmethod
    async def generate_call_summary(
        self,
        transcripts: List[Dict[str, Any]],
        caller_name: Optional[str] = None,
        caller_number: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Generate structured summary, urgency, and action items from transcripts."""
        pass


class GoogleGenAiLiveSession(GeminiLiveSession):
    """Real implementation of GeminiLiveSession using google-genai SDK."""

    def __init__(self, raw_session: Any, raw_ctx: Optional[Any] = None):
        self._session = raw_session
        self._ctx = raw_ctx
        self._closed = False

    async def send_audio(self, pcm_bytes: bytes) -> None:
        if self._closed or not pcm_bytes:
            return
        from google.genai import types

        try:
            if hasattr(self._session, "send_realtime_input"):
                await self._session.send_realtime_input(
                    media=types.Blob(data=pcm_bytes, mime_type="audio/pcm;rate=16000")
                )
            else:
                realtime_input = types.LiveClientRealtimeInput(
                    media_chunks=[types.Blob(data=pcm_bytes, mime_type="audio/pcm;rate=16000")]
                )
                await self._session.send(input=realtime_input)
        except Exception as e:
            if not self._closed:
                logger.debug("Failed sending audio chunk to Gemini: %s", e)

    async def send_text(self, text: str) -> None:
        if self._closed or not text:
            return
        from google.genai import types

        try:
            if hasattr(self._session, "send_client_content"):
                await self._session.send_client_content(
                    turns=types.Content(
                        role="user",
                        parts=[types.Part.from_text(text=text)],
                    ),
                    turn_complete=True,
                )
            else:
                await self._session.send(input=text, end_of_turn=True)
        except Exception as e:
            if not self._closed:
                logger.debug("Failed sending text to Gemini: %s", e)

    async def receive_events(self) -> AsyncGenerator[LiveEvent, None]:
        try:
            while not self._closed:
                try:
                    async for message in self._session.receive():
                        if self._closed:
                            break

                        server_content = getattr(message, "server_content", None)
                        if not server_content:
                            continue

                        # Check for barge-in / interruption
                        if getattr(server_content, "interrupted", False):
                            yield LiveInterrupted()

                        # Model turn audio parts
                        model_turn = getattr(server_content, "model_turn", None)
                        if model_turn:
                            parts = getattr(model_turn, "parts", []) or []
                            for part in parts:
                                # Extract audio chunk
                                inline_data = getattr(part, "inline_data", None)
                                if inline_data and getattr(inline_data, "data", None):
                                    yield LiveAudioChunk(data=inline_data.data)

                        # Input transcription (caller speech recognized by Gemini STT)
                        input_tx = getattr(server_content, "input_transcription", None)
                        if input_tx:
                            tx_text = getattr(input_tx, "text", "") or ""
                            if tx_text:
                                is_final = bool(getattr(input_tx, "finished", False))
                                yield LiveTranscriptChunk(speaker="caller", text=tx_text, is_final=is_final)

                        # Output transcription (assistant speech recognized by Gemini)
                        output_tx = getattr(server_content, "output_transcription", None)
                        if output_tx:
                            tx_text = getattr(output_tx, "text", "") or ""
                            if tx_text:
                                is_final = bool(getattr(output_tx, "finished", False))
                                yield LiveTranscriptChunk(speaker="assistant", text=tx_text, is_final=is_final)
                        elif model_turn:
                            # Fallback text delta only if output_transcription was not sent and part is not a thought
                            for part in getattr(model_turn, "parts", []) or []:
                                if not getattr(part, "thought", False):
                                    text = getattr(part, "text", None)
                                    if text:
                                        yield LiveTranscriptChunk(speaker="assistant", text=text, is_final=False)

                        # Turn completion
                        if getattr(server_content, "turn_complete", False):
                            yield LiveTurnComplete()

                except asyncio.CancelledError:
                    break
                except Exception as e:
                    if self._closed:
                        break
                    err_str = str(e)
                    if "1000" in err_str or "ConnectionClosed" in type(e).__name__:
                        logger.info("Gemini Live connection closed.")
                        break
                    else:
                        logger.warning("Gemini Live receive stream encountered exception: %s (%s)", type(e).__name__, e)
                        raise

        except asyncio.CancelledError:
            pass

    async def close(self) -> None:
        self._closed = True
        try:
            if hasattr(self._session, "close"):
                await self._session.close()
        except Exception as e:
            logger.debug("Exception closing Gemini Live session: %s", e)
        try:
            if self._ctx and hasattr(self._ctx, "__aexit__"):
                await self._ctx.__aexit__(None, None, None)
        except Exception as e:
            logger.debug("Exception exiting Gemini Live context: %s", e)


class DefaultGeminiLiveService(GeminiLiveService):
    """Production implementation using google-genai SDK with safe fallback summary parsing."""

    def __init__(self, api_key: Optional[str] = None, model: Optional[str] = None):
        self.api_key = api_key or settings.GEMINI_API_KEY
        self.model = model or settings.GEMINI_MODEL
        self._client: Optional[Any] = None

    def _get_client(self) -> Any:
        if self._client is None:
            from google import genai

            self._client = genai.Client(api_key=self.api_key)
        return self._client

    async def connect_session(
        self,
        voice: Optional[str] = None,
        custom_instructions: Optional[str] = None,
    ) -> GeminiLiveSession:
        if not settings.is_gemini_configured():
            raise RuntimeError(
                "Gemini Live API is not configured. Please set a valid GEMINI_API_KEY in environment variables."
            )

        from google.genai import types

        client = self._get_client()
        chosen_voice = voice or settings.GEMINI_DEFAULT_VOICE
        system_instruction = custom_instructions or CALL_SCREENING_SYSTEM_INSTRUCTION

        config = types.LiveConnectConfig(
            response_modalities=[types.Modality.AUDIO],
            thinking_config=types.ThinkingConfig(thinking_budget=0),
            speech_config=types.SpeechConfig(
                voice_config=types.VoiceConfig(
                    prebuilt_voice_config=types.PrebuiltVoiceConfig(voice_name=chosen_voice)
                )
            ),
            system_instruction=types.Content(parts=[types.Part.from_text(text=system_instruction)]),
            input_audio_transcription=types.AudioTranscriptionConfig(),
            output_audio_transcription=types.AudioTranscriptionConfig(),
            realtime_input_config=types.RealtimeInputConfig(
                automatic_activity_detection=types.AutomaticActivityDetection(
                    disabled=False,
                    silence_duration_ms=600,
                    start_of_speech_sensitivity=types.StartSensitivity.START_SENSITIVITY_HIGH,
                    end_of_speech_sensitivity=types.EndSensitivity.END_SENSITIVITY_HIGH,
                )
            ),
        )

        raw_session_ctx = client.aio.live.connect(model=self.model, config=config)
        raw_session = await raw_session_ctx.__aenter__()
        return GoogleGenAiLiveSession(raw_session, raw_ctx=raw_session_ctx)

    async def generate_call_summary(
        self,
        transcripts: List[Dict[str, Any]],
        caller_name: Optional[str] = None,
        caller_number: Optional[str] = None,
    ) -> Dict[str, Any]:
        """
        Extracts structured call summary, urgency rating, key facts, and action items.
        Attempts Gemini model structured output; falls back gracefully if Gemini is offline or unavailable.
        """
        # If no transcripts exist, return default minimal summary
        if not transcripts:
            return {
                "summary": "Call ended without recorded dialog.",
                "urgency": "low",
                "urgency_explanation": "No conversation was recorded.",
                "caller_reason": "No reason specified.",
                "uncertainties": None,
                "key_facts": [],
                "action_items": [],
            }

        # Build conversation text
        dialogue = "\n".join(
            f"{t.get('speaker', 'unknown').capitalize()}: {t.get('text', '')}"
            for t in sorted(transcripts, key=lambda x: x.get("sequence_order", 0))
        )

        # Attempt structured summary using Gemini generate_content if configured
        if settings.is_gemini_configured():
            try:
                client = self._get_client()
                prompt = f"""You are the CallMate AI Screening Intelligence engine. Analyze this completed phone conversation between a caller and CallMate assistant.

Caller Name: {caller_name or 'Unknown'}
Caller Number: {caller_number or 'Unknown'}

Transcript:
{dialogue}

Return a valid JSON object matching this schema:
{{
  "summary": "1-2 sentence concise, informative summary of the call",
  "urgency": "low" | "medium" | "high" | "critical",
  "urgency_explanation": "brief 1-sentence explanation for the urgency rating",
  "caller_reason": "concise description of why the caller called",
  "uncertainties": "any ambiguous or unverified details, or null",
  "key_facts": ["fact 1", "fact 2"],
  "action_items": ["action item 1", "action item 2"]
}}

Instructions for action_items:
- Extract concrete follow-up tasks, commitments, callbacks, reminders, or items requested during the call (e.g., 'Remind user about project meeting tomorrow at 10 AM', 'Send requested invoice to John', 'Bring project report').
- Include dates and times only when explicitly stated in the conversation.
- Do NOT generate vague or non-actionable tasks. If no follow-up is needed, return []."""

                summary_model = getattr(settings, "GEMINI_SUMMARY_MODEL", "gemini-2.5-flash")
                response = await asyncio.wait_for(
                    client.aio.models.generate_content(
                        model=summary_model,
                        contents=prompt,
                        config={"response_mime_type": "application/json"},
                    ),
                    timeout=8.0,
                )
                text = getattr(response, "text", "") or ""
                parsed = json.loads(text)
                return {
                    "summary": parsed.get("summary") or "Screened call completed.",
                    "urgency": parsed.get("urgency", "low").lower() if parsed.get("urgency", "low").lower() in ["low", "medium", "high", "critical"] else "low",
                    "urgency_explanation": parsed.get("urgency_explanation") or "Automated call analysis.",
                    "caller_reason": parsed.get("caller_reason") or "Inquiry.",
                    "uncertainties": parsed.get("uncertainties"),
                    "key_facts": [str(f) for f in parsed.get("key_facts", []) if f],
                    "action_items": [str(a) for a in parsed.get("action_items", []) if a],
                }
            except Exception as e:
                logger.warning("Gemini structured summary failed (%s), using fallback heuristic.", e)

        # Robust heuristic fallback when Gemini is unavailable or during tests
        return self._heuristic_fallback_summary(transcripts, caller_name, caller_number)

    def _heuristic_fallback_summary(
        self,
        transcripts: List[Dict[str, Any]],
        caller_name: Optional[str] = None,
        caller_number: Optional[str] = None,
    ) -> Dict[str, Any]:
        caller_turns = [t.get("text", "") for t in transcripts if t.get("speaker") == "caller"]
        assistant_turns = [t.get("text", "") for t in transcripts if t.get("speaker") == "assistant"]

        full_caller_text = " ".join(caller_turns).lower()
        urgency = "low"
        urgency_explanation = "Routine call screening."

        if any(w in full_caller_text for w in ["emergency", "hospital", "critical", "danger"]):
            urgency = "critical"
            urgency_explanation = "Caller mentioned emergency or critical keywords."
        elif any(w in full_caller_text for w in ["urgent", "asap", "immediately", "today"]):
            urgency = "high"
            urgency_explanation = "Caller requested urgent or immediate attention."
        elif any(w in full_caller_text for w in ["important", "deliver", "parcel", "appointment"]):
            urgency = "medium"
            urgency_explanation = "Caller mentioned scheduled delivery or appointment."

        summary_text = (
            f"Screened call from {caller_name or caller_number or 'caller'} with {len(transcripts)} turns."
        )
        if caller_turns:
            first_turn = caller_turns[0]
            summary_text = f"Call from {caller_name or 'caller'}: \"{first_turn[:120]}\""

        action_items = []
        if any(w in full_caller_text for w in ["call back", "call me", "reach me"]):
            action_items.append(f"Return call to {caller_name or caller_number or 'caller'}")
        if any(w in full_caller_text for w in ["meet", "meeting", "sche du led", "scheduled", "schedule", "appointment"]):
            action_items.append(f"Follow up on scheduled meeting with {caller_name or 'caller'}")
        if any(w in full_caller_text for w in ["send", "email", "share", "doc", "invoice", "report"]):
            action_items.append(f"Send requested documents/information to {caller_name or 'caller'}")
        if any(w in full_caller_text for w in ["deliver", "delivery", "parcel", "courier", "package"]):
            action_items.append(f"Receive delivery from {caller_name or 'courier'}")
        if any(w in full_caller_text for w in ["pay", "payment", "bill", "due"]):
            action_items.append(f"Review payment/bill from {caller_name or 'caller'}")

        return {
            "summary": summary_text,
            "urgency": urgency,
            "urgency_explanation": urgency_explanation,
            "caller_reason": caller_turns[0][:100] if caller_turns else "Screened incoming call.",
            "uncertainties": None,
            "key_facts": [f"Total turns: {len(transcripts)}"],
            "action_items": action_items,
        }


# Dependency injection singleton / factory
_gemini_service_instance: Optional[GeminiLiveService] = None


def get_gemini_live_service() -> GeminiLiveService:
    global _gemini_service_instance
    if _gemini_service_instance is None:
        _gemini_service_instance = DefaultGeminiLiveService()
    return _gemini_service_instance


def set_gemini_live_service(service: Optional[GeminiLiveService]) -> None:
    """Allows test fixtures to inject mock GeminiLiveService."""
    global _gemini_service_instance
    _gemini_service_instance = service
