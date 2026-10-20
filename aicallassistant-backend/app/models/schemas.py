from datetime import datetime, timezone
from typing import Any, Dict, List, Literal, Optional
from pydantic import BaseModel, Field, field_validator

# Type definitions matching database constraints in 20261018160800_initial_schema.sql
CallStatus = Literal["connecting", "active", "completed", "failed", "cancelled"]
CallType = Literal["simulated", "cellular_forwarded"]
UrgencyLevel = Literal["low", "medium", "high", "critical"]
SpeakerType = Literal["caller", "assistant"]


class HealthResponse(BaseModel):
    status: str = "healthy"
    version: str = "0.1.0"
    environment: str
    gemini_configured: bool = False
    timestamp: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))


class ErrorDetail(BaseModel):
    code: str
    message: str
    details: Optional[Any] = None


class ErrorEnvelope(BaseModel):
    error: ErrorDetail


class DeleteResponse(BaseModel):
    deleted: bool = True
    id: str


# =============================================================================
# Calls Schemas
# =============================================================================

class SimulateCallRequest(BaseModel):
    caller_name: Optional[str] = Field(default="Delivery Agent (Simulated)", max_length=100)
    caller_number: str = Field(default="+919876543210", min_length=3, max_length=30)
    voice_persona: Optional[str] = Field(default="Puck", max_length=50)
    language: Optional[str] = Field(default="en-IN", max_length=10)


class SimulateCallResponse(BaseModel):
    call_id: str
    status: CallStatus = "connecting"
    ws_url: str
    created_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))


class CallListItem(BaseModel):
    id: str
    caller_name: Optional[str] = None
    caller_number: str
    status: CallStatus
    call_type: CallType = "simulated"
    caller_reason: Optional[str] = None
    summary: Optional[str] = None
    urgency: UrgencyLevel = "low"
    is_important: bool = False
    duration_seconds: int = 0
    has_recording: bool = False
    created_at: datetime


class CallListResponse(BaseModel):
    calls: List[CallListItem]
    total: int
    limit: int
    offset: int


class CallUpdateRequest(BaseModel):
    is_important: Optional[bool] = None
    status: Optional[CallStatus] = None
    caller_reason: Optional[str] = None
    summary: Optional[str] = None
    urgency: Optional[UrgencyLevel] = None
    urgency_explanation: Optional[str] = None
    uncertainties: Optional[str] = None
    key_facts: Optional[List[Any]] = None
    duration_seconds: Optional[int] = Field(default=None, ge=0)


# =============================================================================
# Transcript Schemas
# =============================================================================

class TranscriptItem(BaseModel):
    id: str
    call_id: str
    speaker: SpeakerType
    text: str
    timestamp_ms: int = Field(default=0, ge=0)
    sequence_order: int = Field(default=0, ge=0)
    created_at: datetime


class CreateTranscriptRequest(BaseModel):
    speaker: SpeakerType
    text: str = Field(..., min_length=1, max_length=5000)
    timestamp_ms: int = Field(default=0, ge=0)
    sequence_order: int = Field(default=0, ge=0)


class TranscriptListResponse(BaseModel):
    call_id: str
    transcripts: List[TranscriptItem]
    total: int


# =============================================================================
# Action Items Schemas
# =============================================================================

class ActionItem(BaseModel):
    id: str
    call_id: str
    task: str
    due_date: Optional[datetime] = None
    completed: bool = False
    completed_at: Optional[datetime] = None
    created_at: datetime


class CreateActionItemRequest(BaseModel):
    task: str = Field(..., min_length=1, max_length=1000)
    due_date: Optional[datetime] = None


class UpdateActionItemRequest(BaseModel):
    completed: Optional[bool] = None
    task: Optional[str] = Field(default=None, min_length=1, max_length=1000)
    due_date: Optional[datetime] = None


class ActionItemListResponse(BaseModel):
    call_id: Optional[str] = None
    action_items: List[ActionItem]
    total: int


# =============================================================================
# Summary Schemas
# =============================================================================

class CallSummaryResponse(BaseModel):
    call_id: str
    summary: Optional[str] = None
    caller_reason: Optional[str] = None
    urgency: UrgencyLevel = "low"
    urgency_explanation: Optional[str] = None
    key_facts: List[Any] = Field(default_factory=list)
    uncertainties: Optional[str] = None
    duration_seconds: int = 0


class UpdateSummaryRequest(BaseModel):
    summary: str = Field(..., min_length=1)
    caller_reason: Optional[str] = None
    urgency: UrgencyLevel = "low"
    urgency_explanation: Optional[str] = None
    key_facts: List[Any] = Field(default_factory=list)
    uncertainties: Optional[str] = None


# =============================================================================
# Complete Call Detail Schema
# =============================================================================

class CallDetailResponse(BaseModel):
    id: str
    caller_name: Optional[str] = None
    caller_number: str
    status: CallStatus
    call_type: CallType = "simulated"
    caller_reason: Optional[str] = None
    summary: Optional[str] = None
    urgency: UrgencyLevel = "low"
    urgency_explanation: Optional[str] = None
    uncertainties: Optional[str] = None
    key_facts: List[Any] = Field(default_factory=list)
    is_important: bool = False
    duration_seconds: int = 0
    recording_path: Optional[str] = None
    recording_duration_seconds: Optional[int] = None
    recording_size_bytes: Optional[int] = None
    has_recording: bool = False
    created_at: datetime
    started_at: Optional[datetime] = None
    ended_at: Optional[datetime] = None
    transcripts: List[TranscriptItem] = Field(default_factory=list)
    action_items: List[ActionItem] = Field(default_factory=list)


# =============================================================================
# Storage / Recording Schemas
# =============================================================================

class SignedUrlResponse(BaseModel):
    call_id: str
    signed_url: str
    expires_at: datetime
    recording_path: str
    file_size_bytes: Optional[int] = None


# =============================================================================
# WebSocket Real-Time Streaming Schemas
# =============================================================================

class AudioInEvent(BaseModel):
    event: Literal["audio_in"] = "audio_in"
    data: str = Field(description="Base64-encoded raw PCM 16kHz 16-bit mono audio")
    timestamp_ms: int = Field(default=0, ge=0)


class InterruptEvent(BaseModel):
    event: Literal["interrupt"] = "interrupt"
    timestamp_ms: int = Field(default=0, ge=0)


class EndCallEvent(BaseModel):
    event: Literal["end_call"] = "end_call"
    reason: str = Field(default="user_hangup")


class CallStartedEvent(BaseModel):
    event: Literal["call_started"] = "call_started"
    call_id: str
    model: str
    voice: str


class AudioOutEvent(BaseModel):
    event: Literal["audio_out"] = "audio_out"
    data: str = Field(description="Base64-encoded raw PCM 24kHz 16-bit mono audio")
    timestamp_ms: int = Field(default=0, ge=0)


class TranscriptDeltaEvent(BaseModel):
    event: Literal["transcript_delta"] = "transcript_delta"
    speaker: SpeakerType
    text: str
    is_final: bool = False
    timestamp_ms: int = Field(default=0, ge=0)


class InterruptedEvent(BaseModel):
    event: Literal["interrupted"] = "interrupted"


class TurnCompleteEvent(BaseModel):
    event: Literal["turn_complete"] = "turn_complete"
    timestamp_ms: int = Field(default=0, ge=0)


class CallEndedEvent(BaseModel):
    event: Literal["call_ended"] = "call_ended"
    call_id: str
    summary: Optional[str] = None
    duration_seconds: int = Field(default=0, ge=0)
    key_facts: List[str] = Field(default_factory=list)
    action_items: List[str] = Field(default_factory=list)
    urgency: Optional[str] = None
    urgency_explanation: Optional[str] = None
    caller_reason: Optional[str] = None


class StreamErrorEvent(BaseModel):
    event: Literal["error"] = "error"
    code: str
    message: str

