import asyncio
import json
import logging
from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any, AsyncGenerator, Dict, List, Optional, Union

from app.core.config import settings

logger = logging.getLogger("aicallassistant.gemini_live")

# System prompt defining the concise, professional AI call-screening persona
CALL_SCREENING_SYSTEM_INSTRUCTION = """You are CallMate, a voice AI assistant answering an incoming call on behalf of an unavailable person.
Operating Guidelines:
1. Identify yourself immediately: Always state clearly upfront that you are CallMate, an AI assistant answering on behalf of the person they are trying to reach.
2. Be conversational, proactive, and engaging: As soon as the caller answers or pauses, acknowledge what they said and naturally ask relevant follow-up questions (e.g., asking for their name, the reason for calling, details about the meeting/topic, urgency, callback number, or any specific message).
3. Do not remain silent when the caller finishes speaking: Actively drive the conversation forward by asking one clear, polite question at a time until you have gathered all necessary information.
4. Keep speaking turns short (1 to 2 sentences per turn), polite, and optimized for natural speech dialogue.
5. Grounded in facts: Do not invent details or assume availability. If the caller asks for the person, explain they are unavailable and offer to pass on their message.
6. When the caller has finished giving their information, summarize what was noted, thank them warmly, and confirm the message will be delivered."""


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
                    audio=types.Blob(data=pcm_bytes, mime_type="audio/pcm;rate=16000")
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
            async for message in self._session.receive():
                if self._closed:
                    break

                server_content = getattr(message, "server_content", None)
                if not server_content:
                    continue

                # Check for barge-in / interruption
                if getattr(server_content, "interrupted", False):
                    yield LiveInterrupted()

                # Model turn audio & text parts
                model_turn = getattr(server_content, "model_turn", None)
                if model_turn:
                    parts = getattr(model_turn, "parts", []) or []
                    for part in parts:
                        # Extract audio chunk
                        inline_data = getattr(part, "inline_data", None)
                        if inline_data and getattr(inline_data, "data", None):
                            yield LiveAudioChunk(data=inline_data.data)

                        # Extract text delta
                        text = getattr(part, "text", None)
                        if text:
                            yield LiveTranscriptChunk(speaker="assistant", text=text, is_final=False)

                # Input transcription (caller transcription from Gemini speech-to-text)
                input_tx = getattr(server_content, "input_transcription", None)
                if input_tx:
                    tx_text = getattr(input_tx, "text", "") or ""
                    if tx_text:
                        yield LiveTranscriptChunk(speaker="caller", text=tx_text, is_final=True)

                # Output transcription (assistant speech transcript)
                output_tx = getattr(server_content, "output_transcription", None)
                if output_tx:
                    tx_text = getattr(output_tx, "text", "") or ""
                    if tx_text:
                        yield LiveTranscriptChunk(speaker="assistant", text=tx_text, is_final=False)

                # Turn completion
                if getattr(server_content, "turn_complete", False):
                    yield LiveTurnComplete()

        except asyncio.CancelledError:
            pass
        except Exception as e:
            err_str = str(e)
            if "1000" in err_str or "ConnectionClosed" in type(e).__name__:
                logger.info("Gemini Live receive stream ended cleanly.")
            else:
                logger.warning("Gemini Live receive stream encountered exception: %s (%s)", type(e).__name__, e)
                raise

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
                    silence_duration_ms=800,
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
                prompt = f"""Analyze this screened phone call transcript.
Caller Name: {caller_name or 'Unknown'}
Caller Number: {caller_number or 'Unknown'}

Transcript:
{dialogue}

Return a valid JSON object matching this schema:
{{
  "summary": "1-2 sentence concise summary of the call",
  "urgency": "low" | "medium" | "high" | "critical",
  "urgency_explanation": "brief explanation for urgency rating",
  "caller_reason": "concise description of why caller called",
  "uncertainties": "any ambiguous or unverified details, or null",
  "key_facts": ["fact 1", "fact 2"],
  "action_items": ["action item 1", "action item 2"]
}}"""
                response = await asyncio.wait_for(
                    client.aio.models.generate_content(
                        model="gemini-3.8-flash",
                        contents=prompt,
                        config={"response_mime_type": "application/json"},
                    ),
                    timeout=10.0,
                )
                text = getattr(response, "text", "") or ""
                parsed = json.loads(text)
                return {
                    "summary": parsed.get("summary") or "Screened call completed.",
                    "urgency": parsed.get("urgency", "low").lower() if parsed.get("urgency", "low").lower() in ["low", "medium", "high", "critical"] else "low",
                    "urgency_explanation": parsed.get("urgency_explanation") or "Automated call analysis.",
                    "caller_reason": parsed.get("caller_reason") or "Inquiry.",
                    "uncertainties": parsed.get("uncertainties"),
                    "key_facts": parsed.get("key_facts") or [],
                    "action_items": parsed.get("action_items") or [],
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
        if any(w in full_caller_text for w in ["send", "email", "share"]):
            action_items.append("Review and send requested information")

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
