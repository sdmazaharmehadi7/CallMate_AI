import logging
from typing import Optional
from fastapi import APIRouter, Depends, Query, WebSocket, status

from app.api.deps import get_db_service, get_token_verifier
from app.core.auth import SupabaseTokenVerifier
from app.core.errors import UnauthorizedException
from app.models.schemas import StreamErrorEvent
from app.services.database import DatabaseService
from app.services.voice_session import VoiceSession

logger = logging.getLogger("aicallassistant.stream")

router = APIRouter(tags=["Voice Streaming"])


async def handle_call_stream(
    websocket: WebSocket,
    call_id: str,
    token: Optional[str] = Query(None),
    verifier: SupabaseTokenVerifier = Depends(get_token_verifier),
    db_service: DatabaseService = Depends(get_db_service),
) -> None:
    """
    WebSocket endpoint establishing an authenticated real-time voice streaming session
    between the Android client and the Gemini Live API.
    """
    # 1. Validate Supabase access token
    if not token or not token.strip():
        await websocket.accept()
        err = StreamErrorEvent(
            code="UNAUTHORIZED",
            message="Missing required 'token' query parameter.",
        )
        await websocket.send_text(err.model_dump_json())
        await websocket.close(code=4401)
        return

    try:
        user = verifier.verify(token.strip())
    except UnauthorizedException as e:
        await websocket.accept()
        err = StreamErrorEvent(
            code="UNAUTHORIZED",
            message=e.message,
        )
        await websocket.send_text(err.model_dump_json())
        await websocket.close(code=4401)
        return

    # 2. Verify call ownership
    call_record = db_service.get_call(call_id, user.id)
    if not call_record:
        logger.warning("User %s attempted unauthorized stream for call %s", user.id, call_id)
        await websocket.accept()
        err = StreamErrorEvent(
            code="FORBIDDEN",
            message="Call record not found or not owned by the authenticated user.",
        )
        await websocket.send_text(err.model_dump_json())
        await websocket.close(code=4403)
        return

    # 3. Launch dedicated voice session
    session = VoiceSession(
        websocket=websocket,
        call_id=call_id,
        user_id=user.id,
        call_record=call_record,
        db_service=db_service,
    )
    await session.run()


# Route matching api_contracts.md: /ws/calls/{call_id}/stream
@router.websocket("/ws/calls/{call_id}/stream")
async def ws_calls_stream(
    websocket: WebSocket,
    call_id: str,
    token: Optional[str] = Query(None),
    verifier: SupabaseTokenVerifier = Depends(get_token_verifier),
    db_service: DatabaseService = Depends(get_db_service),
):
    await handle_call_stream(websocket, call_id, token, verifier, db_service)


# Additional route under /api/v1: /api/v1/calls/{call_id}/stream
@router.websocket("/calls/{call_id}/stream")
async def api_calls_stream(
    websocket: WebSocket,
    call_id: str,
    token: Optional[str] = Query(None),
    verifier: SupabaseTokenVerifier = Depends(get_token_verifier),
    db_service: DatabaseService = Depends(get_db_service),
):
    await handle_call_stream(websocket, call_id, token, verifier, db_service)
