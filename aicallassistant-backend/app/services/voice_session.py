import asyncio
import base64
import io
import json
import logging
import struct
import wave
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
from fastapi import WebSocket, WebSocketDisconnect

from app.core.config import settings
from app.models.schemas import (
    AudioInEvent,
    AudioOutEvent,
    CallEndedEvent,
    CallStartedEvent,
    CreateActionItemRequest,
    CreateTranscriptRequest,
    EndCallEvent,
    InterruptedEvent,
    InterruptEvent,
    StreamErrorEvent,
    TranscriptDeltaEvent,
    UpdateSummaryRequest,
)
from app.services.database import DatabaseService
from app.services.gemini_live import (
    GeminiLiveService,
    GeminiLiveSession,
    LiveAudioChunk,
    LiveInterrupted,
    LiveTranscriptChunk,
    LiveTurnComplete,
    get_gemini_live_service,
)

logger = logging.getLogger("aicallassistant.voice_session")


def resample_pcm16_mono(pcm_bytes: bytes, in_rate: int, out_rate: int) -> bytes:
    """Linear interpolation resampler for 16-bit mono PCM."""
    if in_rate == out_rate or not pcm_bytes:
        return pcm_bytes
    num_samples = len(pcm_bytes) // 2
    if num_samples == 0:
        return b""
    samples = struct.unpack(f"<{num_samples}h", pcm_bytes)
    out_samples_len = int(num_samples * out_rate / in_rate)
    ratio = in_rate / out_rate
    out_samples = [0] * out_samples_len
    for i in range(out_samples_len):
        orig_idx = i * ratio
        idx = int(orig_idx)
        frac = orig_idx - idx
        if idx + 1 < num_samples:
            val = int(samples[idx] * (1 - frac) + samples[idx + 1] * frac)
        else:
            val = samples[min(idx, num_samples - 1)]
        out_samples[i] = max(-32768, min(32767, val))
    return struct.pack(f"<{out_samples_len}h", *out_samples)


class ActiveSessionRegistry:
    """Thread-safe in-memory tracker preventing duplicate concurrent sessions for the same call."""

    def __init__(self):
        self._active_calls: Dict[str, str] = {}  # call_id -> user_id
        self._lock = asyncio.Lock()

    async def acquire(self, call_id: str, user_id: str) -> bool:
        async with self._lock:
            if call_id in self._active_calls:
                return False
            self._active_calls[call_id] = user_id
            return True

    async def release(self, call_id: str) -> None:
        async with self._lock:
            self._active_calls.pop(call_id, None)

    async def is_active(self, call_id: str) -> bool:
        async with self._lock:
            return call_id in self._active_calls


# Global registry
session_registry = ActiveSessionRegistry()


class VoiceSession:
    """Manages an active real-time call screening session between Android client and Gemini Live."""

    def __init__(
        self,
        websocket: WebSocket,
        call_id: str,
        user_id: str,
        call_record: Dict[str, Any],
        db_service: DatabaseService,
        gemini_service: Optional[GeminiLiveService] = None,
    ):
        self.websocket = websocket
        self.call_id = call_id
        self.user_id = user_id
        self.call_record = call_record
        self.db_service = db_service
        self.gemini_service = gemini_service or get_gemini_live_service()

        self.started_at: Optional[datetime] = None
        self.ended_at: Optional[datetime] = None
        self.transcripts: List[Dict[str, Any]] = []
        self._next_sequence: int = 0
        self._active_gemini_session: Optional[GeminiLiveSession] = None
        self._closed: bool = False
        self._finalized: bool = False
        self._is_accepted: bool = False
        self._is_started: bool = False
        self._recorded_audio_chunks: List[bytes] = []

    async def run(self) -> None:
        """Main lifecycle of the voice streaming session."""
        acquired = await session_registry.acquire(self.call_id, self.user_id)
        if not acquired:
            logger.warning("Duplicate session attempted for call %s by user %s", self.call_id, self.user_id)
            await self._send_error_and_close(
                code="SESSION_CONFLICT",
                message="An active streaming session already exists for this call.",
                close_code=4409,
            )
            return

        try:
            # 1. Accept WebSocket connection
            await self.websocket.accept()
            self._is_accepted = True

            # 2. Check Gemini API configuration
            if not settings.is_gemini_configured():
                logger.error("Gemini API is not configured on backend.")
                await self._send_error_and_close(
                    code="GEMINI_NOT_CONFIGURED",
                    message="Gemini Live voice engine is not configured on this server.",
                    close_code=4503,
                )
                return

            # 3. Mark call active in database
            self.started_at = datetime.now(timezone.utc)
            self.db_service.update_call(
                self.call_id,
                self.user_id,
                {
                    "status": "active",
                    "started_at": self.started_at,
                },
            )

            # 4. Connect to Gemini Live
            voice_name = self.call_record.get("voice_persona") or settings.GEMINI_DEFAULT_VOICE
            try:
                self._active_gemini_session = await asyncio.wait_for(
                    self.gemini_service.connect_session(voice=voice_name),
                    timeout=float(settings.GEMINI_LIVE_CONNECTION_TIMEOUT_SECONDS),
                )
            except Exception as e:
                logger.error("Failed to connect to Gemini Live for call %s: %s", self.call_id, e)
                self.db_service.update_call(
                    self.call_id,
                    self.user_id,
                    {"status": "failed", "ended_at": datetime.now(timezone.utc)},
                )
                await self._send_error_and_close(
                    code="GEMINI_CONNECTION_FAILED",
                    message="Failed to establish connection with AI voice engine.",
                    close_code=4502,
                )
                return

            # 5. Handshake acknowledgment to Android client
            started_event = CallStartedEvent(
                call_id=self.call_id,
                model=settings.GEMINI_MODEL,
                voice=voice_name,
            )
            await self.websocket.send_text(started_event.model_dump_json())
            self._is_started = True

            # 6. Immediately trigger opening greeting from AI assistant
            asyncio.create_task(self._trigger_greeting())

            # 7. Run bidirectional audio streaming loop with session timeout
            pump_tasks = [
                asyncio.create_task(self._client_to_gemini_pump()),
                asyncio.create_task(self._gemini_to_client_pump()),
            ]

            done, pending = await asyncio.wait(
                pump_tasks,
                timeout=float(settings.GEMINI_SESSION_MAX_DURATION_SECONDS),
                return_when=asyncio.FIRST_COMPLETED,
            )

            for task in pending:
                task.cancel()

        except WebSocketDisconnect:
            logger.info("Client disconnected from call %s", self.call_id)
        except Exception as e:
            logger.exception("Unexpected error in voice session %s: %s", self.call_id, e)
            try:
                await self._send_error(code="INTERNAL_ERROR", message="An unexpected error occurred during the call.")
            except Exception:
                pass
        finally:
            if self._is_started and not self._finalized:
                await self._finalize_session()
            await session_registry.release(self.call_id)

    async def _trigger_greeting(self) -> None:
        """Prompts Gemini Live to immediately speak the opening greeting to the caller."""
        if not self._active_gemini_session or self._closed:
            return
        # Small delay to ensure client audio subsystem is ready
        await asyncio.sleep(0.15)
        try:
            greeting_prompt = (
                "[Call answered by CallMate AI Assistant]. Immediately greet the caller out loud right now: "
                "'Hello! You've reached CallMate, an AI assistant for the person you're trying to reach. "
                "They're unavailable right now. May I know your name and what you're calling about?' "
                "After greeting, listen attentively and ask follow-up questions to gather all the details."
            )
            await self._active_gemini_session.send_text(greeting_prompt)
            logger.info("Triggered AI opening greeting for call %s", self.call_id)
        except Exception as e:
            logger.warning("Failed to trigger initial greeting for call %s: %s", self.call_id, e)

    async def _client_to_gemini_pump(self) -> None:
        """Receives audio and control frames from Android and forwards to Gemini Live."""
        try:
            while not self._closed:
                try:
                    text_data = await self.websocket.receive_text()
                except WebSocketDisconnect:
                    logger.info("Client disconnected WebSocket for call %s", self.call_id)
                    self._closed = True
                    break
                except Exception as e:
                    if not self._closed:
                        logger.debug("Error receiving text from client: %s", e)
                    self._closed = True
                    break

                if not text_data:
                    continue

                try:
                    payload = json.loads(text_data)
                except Exception:
                    await self._send_error("INVALID_JSON", "Received malformed JSON message.")
                    continue

                event_type = payload.get("event")
                if event_type == "audio_in":
                    data_b64 = payload.get("data", "")
                    if not data_b64:
                        continue

                    # Enforce payload size limit
                    if len(data_b64) > (settings.GEMINI_MAX_AUDIO_CHUNK_BYTES * 4 // 3 + 100):
                        await self._send_error("AUDIO_CHUNK_TOO_LARGE", "Audio chunk exceeds maximum permitted size.")
                        continue

                    try:
                        pcm_bytes = base64.b64decode(data_b64)
                    except Exception:
                        await self._send_error("INVALID_AUDIO_DATA", "Failed to decode base64 audio data.")
                        continue

                    if self._active_gemini_session and not self._closed:
                        # Save caller audio chunk (16kHz PCM16) for call recording
                        self._recorded_audio_chunks.append(pcm_bytes)
                        try:
                            await self._active_gemini_session.send_audio(pcm_bytes)
                        except Exception as e:
                            if not self._closed:
                                logger.debug("Error forwarding audio to Gemini: %s", e)

                elif event_type == "interrupt":
                    # User barge-in notification from client
                    if self._active_gemini_session and not self._closed:
                        try:
                            await self._active_gemini_session.send_text("User is speaking.")
                        except Exception as e:
                            logger.debug("Error forwarding interrupt to Gemini: %s", e)

                elif event_type == "end_call":
                    logger.info("Client requested graceful end_call for %s", self.call_id)
                    self._closed = True
                    break
                else:
                    await self._send_error("UNKNOWN_EVENT", f"Unsupported event type: '{event_type}'")
        except asyncio.CancelledError:
            pass
        except Exception as e:
            if not self._closed:
                logger.warning("Unexpected error in client_to_gemini_pump: %s", e)

    async def _gemini_to_client_pump(self) -> None:
        """Receives audio chunks, transcripts, and interruption events from Gemini Live and streams to Android."""
        if not self._active_gemini_session:
            return

        try:
            async for event in self._active_gemini_session.receive_events():
                if self._closed:
                    break

                if isinstance(event, LiveAudioChunk):
                    # Save AI assistant audio resampled to 16kHz for call recording
                    try:
                        resampled_ai = resample_pcm16_mono(event.data, in_rate=24000, out_rate=16000)
                        self._recorded_audio_chunks.append(resampled_ai)
                    except Exception as err:
                        logger.debug("Failed to resample AI chunk for recording: %s", err)

                    # Encode 24kHz PCM chunk as base64 for client
                    b64_audio = base64.b64encode(event.data).decode("utf-8")
                    out_msg = AudioOutEvent(
                        data=b64_audio,
                        timestamp_ms=self._current_elapsed_ms(),
                    )
                    await self.websocket.send_text(out_msg.model_dump_json())

                elif isinstance(event, LiveTranscriptChunk):
                    delta_msg = TranscriptDeltaEvent(
                        speaker=event.speaker,  # type: ignore[arg-type]
                        text=event.text,
                        is_final=event.is_final,
                        timestamp_ms=self._current_elapsed_ms(),
                    )
                    await self.websocket.send_text(delta_msg.model_dump_json())

                    # Collect completed turns for persistence
                    self._record_transcript_turn(event.speaker, event.text)

                elif isinstance(event, LiveInterrupted):
                    interrupt_msg = InterruptedEvent()
                    await self.websocket.send_text(interrupt_msg.model_dump_json())

        except asyncio.CancelledError:
            pass
        except Exception as e:
            logger.warning("Error in gemini_to_client_pump: %s", e)

        # Keep pump alive until client disconnects or hangs up
        while not self._closed:
            await asyncio.sleep(0.05)

    def _record_transcript_turn(self, speaker: str, text: str) -> None:
        text = text.strip()
        if not text:
            return

        # If previous turn was from same speaker, aggregate text
        if self.transcripts and self.transcripts[-1]["speaker"] == speaker:
            self.transcripts[-1]["text"] = f"{self.transcripts[-1]['text']} {text}".strip()
        else:
            self.transcripts.append({
                "speaker": speaker,
                "text": text,
                "timestamp_ms": self._current_elapsed_ms(),
                "sequence_order": self._next_sequence,
            })
            self._next_sequence += 1

    def _current_elapsed_ms(self) -> int:
        if not self.started_at:
            return 0
        diff = datetime.now(timezone.utc) - self.started_at
        return int(diff.total_seconds() * 1000)

    async def _finalize_session(self) -> None:
        """Persists transcripts, generates summary facts, and finalizes call record."""
        if self._finalized:
            return
        self._finalized = True
        self._closed = True
        self.ended_at = datetime.now(timezone.utc)

        # 1. Close Gemini Live session cleanly
        if self._active_gemini_session:
            await self._active_gemini_session.close()

        # 2. Calculate duration
        duration = 0
        if self.started_at and self.ended_at:
            duration = max(0, int((self.ended_at - self.started_at).total_seconds()))

        # 3. Persist ordered transcripts to database
        for item in self.transcripts:
            try:
                self.db_service.add_transcript(
                    self.call_id,
                    self.user_id,
                    CreateTranscriptRequest(
                        speaker=item["speaker"],
                        text=item["text"],
                        timestamp_ms=item.get("timestamp_ms", 0),
                        sequence_order=item.get("sequence_order", 0),
                    ),
                )
            except Exception as e:
                logger.error("Failed to persist transcript for call %s: %s", self.call_id, e)

        # 4. Generate structured summary & action items
        summary_data: Dict[str, Any] = {}
        try:
            summary_data = await self.gemini_service.generate_call_summary(
                transcripts=self.transcripts,
                caller_name=self.call_record.get("caller_name"),
                caller_number=self.call_record.get("caller_number"),
            )
        except Exception as e:
            logger.warning("Summary generation failed: %s", e)
            summary_data = {
                "summary": "Screened call completed.",
                "urgency": "low",
                "urgency_explanation": "Automated call completed.",
                "caller_reason": "Inquiry.",
                "action_items": [],
            }

        # 5. Persist summary to calls table
        try:
            self.db_service.update_summary(
                self.call_id,
                self.user_id,
                UpdateSummaryRequest(
                    caller_reason=summary_data.get("caller_reason"),
                    summary=summary_data.get("summary"),
                    urgency=summary_data.get("urgency", "low"),
                    urgency_explanation=summary_data.get("urgency_explanation"),
                    uncertainties=summary_data.get("uncertainties"),
                    key_facts=summary_data.get("key_facts", []),
                ),
            )
        except Exception as e:
            logger.error("Failed to update call summary for call %s: %s", self.call_id, e)

        # 6. Persist action items
        for action_task in summary_data.get("action_items", []):
            try:
                self.db_service.create_action_item(
                    self.call_id,
                    self.user_id,
                    CreateActionItemRequest(task=action_task),
                )
            except Exception as e:
                logger.error("Failed to create action item: %s", e)

        # 7. Package and upload call recording WAV if any audio was exchanged
        if self._recorded_audio_chunks:
            try:
                raw_pcm = b"".join(self._recorded_audio_chunks)
                wav_buffer = io.BytesIO()
                with wave.open(wav_buffer, "wb") as wf:
                    wf.setnchannels(1)
                    wf.setsampwidth(2)
                    wf.setframerate(16000)
                    wf.writeframes(raw_pcm)
                wav_bytes = wav_buffer.getvalue()

                recording_meta = self.db_service.upload_recording(
                    call_id=self.call_id,
                    user_id=self.user_id,
                    wav_bytes=wav_bytes,
                    duration_seconds=duration,
                )
                logger.info("Successfully recorded and uploaded audio for call %s: %s", self.call_id, recording_meta)
            except Exception as e:
                logger.error("Failed to build/upload recording for call %s: %s", self.call_id, e)

        # 8. Update call status to completed
        try:
            self.db_service.update_call(
                self.call_id,
                self.user_id,
                {
                    "status": "completed",
                    "ended_at": self.ended_at,
                    "duration_seconds": duration,
                },
            )
        except Exception as e:
            logger.error("Failed to finalize call status for %s: %s", self.call_id, e)

        # 8. Send call_ended event to Android client
        try:
            end_event = CallEndedEvent(
                call_id=self.call_id,
                summary=summary_data.get("summary"),
                duration_seconds=duration,
            )
            await self.websocket.send_text(end_event.model_dump_json())
        except Exception:
            pass

        # 9. Close WebSocket cleanly
        try:
            await self.websocket.close(code=1000)
        except Exception:
            pass

    async def _send_error(self, code: str, message: str) -> None:
        err = StreamErrorEvent(code=code, message=message)
        await self.websocket.send_text(err.model_dump_json())

    async def _send_error_and_close(self, code: str, message: str, close_code: int = 4000) -> None:
        self._closed = True
        self._finalized = True
        try:
            if not self._is_accepted:
                await self.websocket.accept()
                self._is_accepted = True
            await self._send_error(code, message)
            await self.websocket.close(code=close_code)
        except Exception as e:
            logger.debug("Exception during _send_error_and_close: %s", e)

