from datetime import datetime, timezone
from typing import Optional
from fastapi import APIRouter, Depends, Query, status

from app.api.deps import get_current_user_id, get_db_service
from app.core.config import settings
from app.core.errors import BadRequestException, NotFoundException
from app.core.utils import validate_status_transition, validate_uuid
from app.models.schemas import (
    ActionItem,
    ActionItemListResponse,
    CallDetailResponse,
    CallListItem,
    CallListResponse,
    CallStatus,
    CallSummaryResponse,
    CallUpdateRequest,
    CreateActionItemRequest,
    CreateTranscriptRequest,
    DeleteResponse,
    SignedUrlResponse,
    SimulateCallRequest,
    SimulateCallResponse,
    TranscriptItem,
    TranscriptListResponse,
    UpdateSummaryRequest,
)
from app.services.database import DatabaseService

router = APIRouter(prefix="/calls", tags=["Calls"])


@router.post(
    "/simulate",
    response_model=SimulateCallResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create simulated call record",
)
async def simulate_call(
    request: SimulateCallRequest,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> SimulateCallResponse:
    """
    Creates a simulated call record for the authenticated user and prepares
    the WebSocket streaming endpoint.
    """
    call_record = db.create_call(user_id, request)
    call_id = call_record["id"]
    ws_url = f"ws://{settings.HOST}:{settings.PORT}/ws/calls/{call_id}/stream"

    return SimulateCallResponse(
        call_id=call_id,
        status="connecting",
        ws_url=ws_url,
        created_at=call_record["created_at"],
    )


@router.get(
    "",
    response_model=CallListResponse,
    summary="List calls for authenticated user",
)
async def list_calls(
    limit: int = Query(20, ge=1, le=100, description="Page limit (1-100)"),
    offset: int = Query(0, ge=0, description="Page offset"),
    status: Optional[CallStatus] = Query(None, description="Filter by status"),
    search: Optional[str] = Query(None, max_length=100, description="Search caller name/number or summary"),
    important_only: Optional[bool] = Query(None, description="Filter only important calls"),
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> CallListResponse:
    """
    Lists the current user's call history ordered newest first, with pagination
    and optional filters.
    """
    records, total = db.list_calls(
        user_id=user_id,
        limit=limit,
        offset=offset,
        status=status,
        search=search,
        important_only=important_only,
    )

    items = []
    for c in records:
        has_rec = bool(c.get("recording_path"))
        items.append(
            CallListItem(
                id=c["id"],
                caller_name=c.get("caller_name"),
                caller_number=c.get("caller_number", "Unknown"),
                status=c["status"],
                call_type=c.get("call_type", "simulated"),
                caller_reason=c.get("caller_reason"),
                summary=c.get("summary"),
                urgency=c.get("urgency", "low"),
                is_important=c.get("is_important", False),
                duration_seconds=c.get("duration_seconds", 0),
                has_recording=has_rec,
                created_at=c["created_at"],
            )
        )

    return CallListResponse(
        calls=items,
        total=total,
        limit=limit,
        offset=offset,
    )


@router.get(
    "/{call_id}",
    response_model=CallDetailResponse,
    summary="Get single call details",
)
async def get_call(
    call_id: str,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> CallDetailResponse:
    """
    Retrieves full details for a single call owned by the authenticated user,
    including turn transcripts and action items.
    """
    valid_id = validate_uuid(call_id, "call_id")
    call = db.get_call(valid_id, user_id)
    if not call:
        raise NotFoundException("Call not found")

    transcripts_raw = db.list_transcripts(valid_id, user_id)
    transcripts = [TranscriptItem(**t) for t in transcripts_raw]

    action_items_raw = db.list_action_items(valid_id, user_id)
    action_items = [ActionItem(**a) for a in action_items_raw]

    has_rec = bool(call.get("recording_path"))

    return CallDetailResponse(
        id=call["id"],
        caller_name=call.get("caller_name"),
        caller_number=call.get("caller_number", "Unknown"),
        status=call["status"],
        call_type=call.get("call_type", "simulated"),
        caller_reason=call.get("caller_reason"),
        summary=call.get("summary"),
        urgency=call.get("urgency", "low"),
        urgency_explanation=call.get("urgency_explanation"),
        uncertainties=call.get("uncertainties"),
        key_facts=call.get("key_facts", []),
        is_important=call.get("is_important", False),
        duration_seconds=call.get("duration_seconds", 0),
        recording_path=call.get("recording_path"),
        recording_duration_seconds=call.get("recording_duration_seconds"),
        recording_size_bytes=call.get("recording_size_bytes"),
        has_recording=has_rec,
        created_at=call["created_at"],
        started_at=call.get("started_at"),
        ended_at=call.get("ended_at"),
        transcripts=transcripts,
        action_items=action_items,
    )


@router.patch(
    "/{call_id}",
    response_model=CallDetailResponse,
    summary="Update permitted call fields",
)
async def update_call(
    call_id: str,
    request: CallUpdateRequest,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> CallDetailResponse:
    """
    Updates mutable call fields such as importance flag, status, or summary.
    Enforces call status state transition rules.
    """
    valid_id = validate_uuid(call_id, "call_id")
    call = db.get_call(valid_id, user_id)
    if not call:
        raise NotFoundException("Call not found")

    updates = request.model_dump(exclude_unset=True)

    # Validate status transitions if status is being updated
    if "status" in updates and updates["status"] is not None:
        new_status = updates["status"]
        validate_status_transition(call["status"], new_status)
        now = datetime.now(timezone.utc)
        if new_status == "active" and not call.get("started_at"):
            updates["started_at"] = now
        elif new_status in ("completed", "failed", "cancelled") and not call.get("ended_at"):
            updates["ended_at"] = now

    updated_call = db.update_call(valid_id, user_id, updates)
    if not updated_call:
        raise NotFoundException("Call not found")

    transcripts_raw = db.list_transcripts(valid_id, user_id)
    transcripts = [TranscriptItem(**t) for t in transcripts_raw]
    action_items_raw = db.list_action_items(valid_id, user_id)
    action_items = [ActionItem(**a) for a in action_items_raw]

    return CallDetailResponse(
        id=updated_call["id"],
        caller_name=updated_call.get("caller_name"),
        caller_number=updated_call.get("caller_number", "Unknown"),
        status=updated_call["status"],
        call_type=updated_call.get("call_type", "simulated"),
        caller_reason=updated_call.get("caller_reason"),
        summary=updated_call.get("summary"),
        urgency=updated_call.get("urgency", "low"),
        urgency_explanation=updated_call.get("urgency_explanation"),
        uncertainties=updated_call.get("uncertainties"),
        key_facts=updated_call.get("key_facts", []),
        is_important=updated_call.get("is_important", False),
        duration_seconds=updated_call.get("duration_seconds", 0),
        recording_path=updated_call.get("recording_path"),
        recording_duration_seconds=updated_call.get("recording_duration_seconds"),
        recording_size_bytes=updated_call.get("recording_size_bytes"),
        has_recording=bool(updated_call.get("recording_path")),
        created_at=updated_call["created_at"],
        started_at=updated_call.get("started_at"),
        ended_at=updated_call.get("ended_at"),
        transcripts=transcripts,
        action_items=action_items,
    )


@router.delete(
    "/{call_id}",
    response_model=DeleteResponse,
    summary="Delete call and associated records",
)
async def delete_call(
    call_id: str,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> DeleteResponse:
    """
    Deletes an owned call, cascading child transcripts and action items,
    and removing any stored audio recording safely.
    """
    valid_id = validate_uuid(call_id, "call_id")
    deleted = db.delete_call(valid_id, user_id)
    if not deleted:
        raise NotFoundException("Call not found")
    return DeleteResponse(deleted=True, id=valid_id)


# =============================================================================
# Transcripts Endpoints
# =============================================================================

@router.get(
    "/{call_id}/transcripts",
    response_model=TranscriptListResponse,
    summary="Get ordered call transcripts",
)
async def get_transcripts(
    call_id: str,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> TranscriptListResponse:
    """
    Retrieves turn-by-turn dialog transcripts for an owned call ordered by sequence.
    """
    valid_id = validate_uuid(call_id, "call_id")
    transcripts_raw = db.list_transcripts(valid_id, user_id)
    items = [TranscriptItem(**t) for t in transcripts_raw]
    return TranscriptListResponse(
        call_id=valid_id,
        transcripts=items,
        total=len(items),
    )


@router.post(
    "/{call_id}/transcripts",
    response_model=TranscriptItem,
    status_code=status.HTTP_201_CREATED,
    summary="Add dialog transcript entry",
)
async def add_transcript(
    call_id: str,
    request: CreateTranscriptRequest,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> TranscriptItem:
    """
    Appends a dialog turn entry to an owned call.
    Enforces duplicate prevention and idempotent retries on sequence order.
    """
    valid_id = validate_uuid(call_id, "call_id")
    created = db.add_transcript(valid_id, user_id, request)
    return TranscriptItem(**created)


# =============================================================================
# Summary Endpoints
# =============================================================================

@router.get(
    "/{call_id}/summary",
    response_model=CallSummaryResponse,
    summary="Get stored call summary",
)
async def get_call_summary(
    call_id: str,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> CallSummaryResponse:
    """
    Returns the stored AI summary, urgency, and key facts.
    Returns 404 if no completed summary exists for the call.
    """
    valid_id = validate_uuid(call_id, "call_id")
    summary = db.get_summary(valid_id, user_id)
    if not summary:
        raise NotFoundException("No completed summary exists for this call")
    return CallSummaryResponse(**summary)


@router.put(
    "/{call_id}/summary",
    response_model=CallSummaryResponse,
    summary="Update or persist call summary",
)
async def update_call_summary(
    call_id: str,
    request: UpdateSummaryRequest,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> CallSummaryResponse:
    """
    Persists a completed call summary, urgency analysis, and extracted facts.
    """
    valid_id = validate_uuid(call_id, "call_id")
    updated = db.update_summary(valid_id, user_id, request)
    return CallSummaryResponse(**updated)


# =============================================================================
# Action Items (Call Parent Scope) Endpoints
# =============================================================================

@router.get(
    "/{call_id}/action-items",
    response_model=ActionItemListResponse,
    summary="List action items for call",
)
async def list_call_action_items(
    call_id: str,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> ActionItemListResponse:
    """
    Lists all follow-up action items created from an owned call.
    """
    valid_id = validate_uuid(call_id, "call_id")
    items_raw = db.list_action_items(valid_id, user_id)
    items = [ActionItem(**a) for a in items_raw]
    return ActionItemListResponse(
        call_id=valid_id,
        action_items=items,
        total=len(items),
    )


@router.post(
    "/{call_id}/action-items",
    response_model=ActionItem,
    status_code=status.HTTP_201_CREATED,
    summary="Create action item for call",
)
async def create_call_action_item(
    call_id: str,
    request: CreateActionItemRequest,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> ActionItem:
    """
    Creates an action item linked to an owned call.
    """
    valid_id = validate_uuid(call_id, "call_id")
    created = db.create_action_item(valid_id, user_id, request)
    return ActionItem(**created)


# =============================================================================
# Recording Access Endpoint
# =============================================================================

@router.get(
    "/{call_id}/recording-url",
    response_model=SignedUrlResponse,
    summary="Get short-lived signed URL for audio playback",
)
async def get_recording_url(
    call_id: str,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> SignedUrlResponse:
    """
    Generates a secure, short-lived signed URL from Supabase Storage for
    playing back the call's audio recording. Verifies ownership and file existence.
    """
    valid_id = validate_uuid(call_id, "call_id")
    result = db.get_recording_signed_url(valid_id, user_id, expires_in=900)
    return SignedUrlResponse(**result)
