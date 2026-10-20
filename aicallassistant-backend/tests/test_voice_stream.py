import asyncio
import base64
import time
import uuid
import pytest
from starlette.websockets import WebSocketDisconnect

from app.core.config import settings
from app.models.schemas import SimulateCallRequest
from app.services.gemini_live import (
    GeminiLiveService,
    GeminiLiveSession,
    LiveAudioChunk,
    LiveInterrupted,
    LiveTranscriptChunk,
    LiveTurnComplete,
    set_gemini_live_service,
)
from app.services.voice_session import session_registry
from tests.conftest import USER_A_ID, USER_B_ID, create_test_token


class MockGeminiLiveSession(GeminiLiveSession):
    def __init__(self, incoming_events=None):
        self.sent_audio = []
        self.sent_text = []
        self.closed = False
        self._events = (
            incoming_events
            if incoming_events is not None
            else [
                LiveAudioChunk(data=b"\x00\x01" * 160),
                LiveTranscriptChunk(speaker="assistant", text="Hello, how can I help you?"),
                LiveTurnComplete(),
            ]
        )

    async def send_audio(self, pcm_bytes: bytes) -> None:
        self.sent_audio.append(pcm_bytes)

    async def send_text(self, text: str) -> None:
        self.sent_text.append(text)

    async def receive_events(self):
        for ev in self._events:
            yield ev
        while not self.closed:
            await asyncio.sleep(0.01)

    async def close(self) -> None:
        self.closed = True


class MockGeminiLiveService(GeminiLiveService):
    def __init__(self, session=None, should_fail_connect=False):
        self.session = session or MockGeminiLiveSession()
        self.should_fail_connect = should_fail_connect
        self.connect_called = False

    async def connect_session(self, voice=None, custom_instructions=None):
        self.connect_called = True
        if self.should_fail_connect:
            raise RuntimeError("Simulated Gemini connection failure")
        return self.session

    async def generate_call_summary(self, transcripts, caller_name=None, caller_number=None):
        return {
            "summary": "Courier arrived with delivery.",
            "urgency": "medium",
            "urgency_explanation": "Delivery requires signature.",
            "caller_reason": "Parcel dropoff.",
            "uncertainties": None,
            "key_facts": ["Delivery arrived at flat 402"],
            "action_items": ["Collect package from reception"],
        }


@pytest.fixture(autouse=True)
def cleanup_registry():
    """Ensure session registry is clean before and after each test."""
    session_registry._active_calls.clear()
    yield
    session_registry._active_calls.clear()
    set_gemini_live_service(None)


@pytest.fixture
def user_a_call(test_db):
    """Creates a call owned by USER_A."""
    return test_db.create_call(
        USER_A_ID,
        SimulateCallRequest(
            caller_name="Test Delivery",
            caller_number="+919876543210",
            voice_persona="Puck",
        ),
    )


def test_ws_stream_missing_token(client, user_a_call):
    call_id = user_a_call["id"]
    with client.websocket_connect(f"/ws/calls/{call_id}/stream") as ws:
        msg = ws.receive_json()
        assert msg["event"] == "error"
        assert msg["code"] == "UNAUTHORIZED"


def test_ws_stream_invalid_token(client, user_a_call):
    call_id = user_a_call["id"]
    with client.websocket_connect(f"/ws/calls/{call_id}/stream?token=invalid.jwt.token") as ws:
        msg = ws.receive_json()
        assert msg["event"] == "error"
        assert msg["code"] == "UNAUTHORIZED"


def test_ws_stream_expired_token(client, user_a_call):
    call_id = user_a_call["id"]
    expired_token = create_test_token(USER_A_ID, expires_in=-10)
    with client.websocket_connect(f"/ws/calls/{call_id}/stream?token={expired_token}") as ws:
        msg = ws.receive_json()
        assert msg["event"] == "error"
        assert msg["code"] == "UNAUTHORIZED"


def test_ws_stream_call_not_found(client):
    token = create_test_token(USER_A_ID)
    random_call_id = str(uuid.uuid4())
    with client.websocket_connect(f"/ws/calls/{random_call_id}/stream?token={token}") as ws:
        msg = ws.receive_json()
        assert msg["event"] == "error"
        assert msg["code"] == "FORBIDDEN"


def test_ws_stream_not_owned_call(client, user_a_call):
    # User B attempts to access User A's call
    user_b_token = create_test_token(USER_B_ID)
    call_id = user_a_call["id"]
    with client.websocket_connect(f"/ws/calls/{call_id}/stream?token={user_b_token}") as ws:
        msg = ws.receive_json()
        assert msg["event"] == "error"
        assert msg["code"] == "FORBIDDEN"


def test_ws_stream_missing_gemini_config(client, user_a_call, monkeypatch):
    monkeypatch.setattr(settings, "GEMINI_API_KEY", "test_placeholder_key")
    token = create_test_token(USER_A_ID)
    call_id = user_a_call["id"]

    with client.websocket_connect(f"/ws/calls/{call_id}/stream?token={token}") as ws:
        msg = ws.receive_json()
        assert msg["event"] == "error"
        assert msg["code"] == "GEMINI_NOT_CONFIGURED"


def test_ws_stream_successful_flow(client, user_a_call, test_db, monkeypatch):
    monkeypatch.setattr(settings, "GEMINI_API_KEY", "real_test_key_valid_format_12345")
    mock_session = MockGeminiLiveSession(
        incoming_events=[
            LiveAudioChunk(data=b"\x01\x02\x03\x04"),
            LiveTranscriptChunk(speaker="assistant", text="Hello from Gemini!"),
            LiveTurnComplete(),
        ]
    )
    mock_service = MockGeminiLiveService(session=mock_session)
    set_gemini_live_service(mock_service)

    token = create_test_token(USER_A_ID)
    call_id = user_a_call["id"]

    with client.websocket_connect(f"/ws/calls/{call_id}/stream?token={token}") as ws:
        # 1. Handshake response
        started_msg = ws.receive_json()
        assert started_msg["event"] == "call_started"
        assert started_msg["call_id"] == call_id
        assert started_msg["voice"] == settings.GEMINI_DEFAULT_VOICE

        # 2. Receive audio out
        audio_out_msg = ws.receive_json()
        assert audio_out_msg["event"] == "audio_out"
        assert base64.b64decode(audio_out_msg["data"]) == b"\x01\x02\x03\x04"

        # 3. Receive transcript delta
        transcript_msg = ws.receive_json()
        assert transcript_msg["event"] == "transcript_delta"
        assert transcript_msg["speaker"] == "assistant"
        assert "Hello from Gemini!" in transcript_msg["text"]

        # 3b. Receive turn_complete
        turn_msg = ws.receive_json()
        assert turn_msg["event"] == "turn_complete"

        # 4. Client streams audio in
        raw_pcm = b"\x00\x00" * 100
        b64_pcm = base64.b64encode(raw_pcm).decode("utf-8")
        ws.send_json({"event": "audio_in", "data": b64_pcm, "timestamp_ms": 100})

        # 5. Client hangs up gracefully
        ws.send_json({"event": "end_call", "reason": "user_hangup"})

        # 6. Receive call_ended event
        end_msg = ws.receive_json()
        assert end_msg["event"] == "call_ended"
        assert end_msg["call_id"] == call_id
        assert "Courier arrived" in end_msg["summary"]

        assert len(mock_session.sent_audio) == 1
        assert mock_session.sent_audio[0] == raw_pcm

    # Verify persistence in database
    call = test_db.get_call(call_id, USER_A_ID)
    assert call["status"] == "completed"
    assert call["summary"] == "Courier arrived with delivery."
    assert call["urgency"] == "medium"

    # Transcripts persisted with sequential ordering
    transcripts = test_db.list_transcripts(call_id, USER_A_ID)
    assert len(transcripts) >= 1
    assert transcripts[0]["speaker"] == "assistant"
    assert "Hello from Gemini!" in transcripts[0]["text"]
    assert transcripts[0]["sequence_order"] == 0

    # Action items persisted
    items = test_db.list_action_items(call_id, USER_A_ID)
    assert len(items) == 1
    assert "Collect package" in items[0]["task"]


def test_ws_stream_interruption_event(client, user_a_call, monkeypatch):
    monkeypatch.setattr(settings, "GEMINI_API_KEY", "real_test_key_valid_format_12345")
    mock_session = MockGeminiLiveSession(
        incoming_events=[
            LiveInterrupted(),
        ]
    )
    mock_service = MockGeminiLiveService(session=mock_session)
    set_gemini_live_service(mock_service)

    token = create_test_token(USER_A_ID)
    call_id = user_a_call["id"]

    with client.websocket_connect(f"/ws/calls/{call_id}/stream?token={token}") as ws:
        # Handshake
        assert ws.receive_json()["event"] == "call_started"

        # Receive interrupted event
        interrupted_msg = ws.receive_json()
        assert interrupted_msg["event"] == "interrupted"

        # Client sends interrupt frame
        ws.send_json({"event": "interrupt", "timestamp_ms": 1200})

        # End call
        ws.send_json({"event": "end_call"})
        assert ws.receive_json()["event"] == "call_ended"

        assert len(mock_session.sent_text) == 1


def test_ws_stream_audio_size_limit(client, user_a_call, monkeypatch):
    monkeypatch.setattr(settings, "GEMINI_API_KEY", "real_test_key_valid_format_12345")
    mock_session = MockGeminiLiveSession(incoming_events=[])
    mock_service = MockGeminiLiveService(session=mock_session)
    set_gemini_live_service(mock_service)

    token = create_test_token(USER_A_ID)
    call_id = user_a_call["id"]

    with client.websocket_connect(f"/ws/calls/{call_id}/stream?token={token}") as ws:
        assert ws.receive_json()["event"] == "call_started"

        # Create audio exceeding 64KB limit
        huge_pcm = b"\x00" * (settings.GEMINI_MAX_AUDIO_CHUNK_BYTES + 5000)
        huge_b64 = base64.b64encode(huge_pcm).decode("utf-8")
        ws.send_json({"event": "audio_in", "data": huge_b64})

        err_msg = ws.receive_json()
        assert err_msg["event"] == "error"
        assert err_msg["code"] == "AUDIO_CHUNK_TOO_LARGE"

        ws.send_json({"event": "end_call"})
        assert ws.receive_json()["event"] == "call_ended"


def test_ws_stream_malformed_json_and_unknown_event(client, user_a_call, monkeypatch):
    monkeypatch.setattr(settings, "GEMINI_API_KEY", "real_test_key_valid_format_12345")
    mock_session = MockGeminiLiveSession(incoming_events=[])
    mock_service = MockGeminiLiveService(session=mock_session)
    set_gemini_live_service(mock_service)

    token = create_test_token(USER_A_ID)
    call_id = user_a_call["id"]

    with client.websocket_connect(f"/ws/calls/{call_id}/stream?token={token}") as ws:
        assert ws.receive_json()["event"] == "call_started"

        # Malformed JSON
        ws.send_text("not a json string{{{")
        err1 = ws.receive_json()
        assert err1["event"] == "error"
        assert err1["code"] == "INVALID_JSON"

        # Unknown event
        ws.send_json({"event": "unsupported_event_xyz"})
        err2 = ws.receive_json()
        assert err2["event"] == "error"
        assert err2["code"] == "UNKNOWN_EVENT"

        ws.send_json({"event": "end_call"})
        assert ws.receive_json()["event"] == "call_ended"


def test_ws_stream_duplicate_session_conflict(client, user_a_call, monkeypatch):
    monkeypatch.setattr(settings, "GEMINI_API_KEY", "real_test_key_valid_format_12345")
    mock_session = MockGeminiLiveSession(incoming_events=[])
    mock_service = MockGeminiLiveService(session=mock_session)
    set_gemini_live_service(mock_service)

    token = create_test_token(USER_A_ID)
    call_id = user_a_call["id"]

    # First session connects
    with client.websocket_connect(f"/ws/calls/{call_id}/stream?token={token}") as ws1:
        assert ws1.receive_json()["event"] == "call_started"

        # Second session for same call attempts connection
        with client.websocket_connect(f"/ws/calls/{call_id}/stream?token={token}") as ws2:
            err = ws2.receive_json()
            assert err["event"] == "error"
            assert err["code"] == "SESSION_CONFLICT"

        ws1.send_json({"event": "end_call"})
        assert ws1.receive_json()["event"] == "call_ended"


def test_ws_stream_gemini_connection_error(client, user_a_call, test_db, monkeypatch):
    monkeypatch.setattr(settings, "GEMINI_API_KEY", "real_test_key_valid_format_12345")
    mock_service = MockGeminiLiveService(should_fail_connect=True)
    set_gemini_live_service(mock_service)

    token = create_test_token(USER_A_ID)
    call_id = user_a_call["id"]

    with client.websocket_connect(f"/ws/calls/{call_id}/stream?token={token}") as ws:
        err = ws.receive_json()
        assert err["event"] == "error"
        assert err["code"] == "GEMINI_CONNECTION_FAILED"

    # Call marked as failed
    call = test_db.get_call(call_id, USER_A_ID)
    assert call["status"] == "failed"
