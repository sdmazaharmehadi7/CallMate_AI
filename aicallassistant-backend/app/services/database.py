import copy
import logging
import uuid
from abc import ABC, abstractmethod
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional, Tuple

from app.core.config import settings
from app.core.errors import (
    AppException,
    BadRequestException,
    ConflictException,
    ForbiddenException,
    NotFoundException,
)
from app.models.schemas import (
    CreateActionItemRequest,
    CreateTranscriptRequest,
    SimulateCallRequest,
    UpdateSummaryRequest,
)

logger = logging.getLogger("aicallassistant.database")


class DatabaseService(ABC):
    """Abstract interface defining all database and storage persistence operations."""

    @abstractmethod
    def create_call(self, user_id: str, data: SimulateCallRequest) -> Dict[str, Any]:
        """Create a simulated call record for the authenticated user."""
        pass

    @abstractmethod
    def list_calls(
        self,
        user_id: str,
        limit: int = 20,
        offset: int = 0,
        status: Optional[str] = None,
        search: Optional[str] = None,
        important_only: Optional[bool] = None,
    ) -> Tuple[List[Dict[str, Any]], int]:
        """List calls owned by the user with pagination and optional filters."""
        pass

    @abstractmethod
    def get_call(self, call_id: str, user_id: str) -> Optional[Dict[str, Any]]:
        """Retrieve a single call record only if owned by user_id."""
        pass

    @abstractmethod
    def update_call(self, call_id: str, user_id: str, updates: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """Update permitted fields on a call record owned by user_id."""
        pass

    @abstractmethod
    def delete_call(self, call_id: str, user_id: str) -> bool:
        """Delete an owned call, cascading child records and cleaning up recording audio."""
        pass

    @abstractmethod
    def list_transcripts(self, call_id: str, user_id: str) -> List[Dict[str, Any]]:
        """Return ordered transcript entries for an owned call."""
        pass

    @abstractmethod
    def add_transcript(self, call_id: str, user_id: str, transcript: CreateTranscriptRequest) -> Dict[str, Any]:
        """Add a turn transcript entry, preventing duplicates on retry."""
        pass

    @abstractmethod
    def get_summary(self, call_id: str, user_id: str) -> Optional[Dict[str, Any]]:
        """Retrieve summary data for an owned call."""
        pass

    @abstractmethod
    def update_summary(self, call_id: str, user_id: str, summary_data: UpdateSummaryRequest) -> Dict[str, Any]:
        """Persist or update summary facts on an owned call."""
        pass

    @abstractmethod
    def list_action_items(self, call_id: str, user_id: str) -> List[Dict[str, Any]]:
        """List action items for an owned call."""
        pass

    @abstractmethod
    def create_action_item(self, call_id: str, user_id: str, item: CreateActionItemRequest) -> Dict[str, Any]:
        """Create an action item for an owned call."""
        pass

    @abstractmethod
    def update_action_item(
        self, action_item_id: str, user_id: str, updates: Dict[str, Any]
    ) -> Optional[Dict[str, Any]]:
        """Update an action item after verifying ownership through its parent call."""
        pass

    @abstractmethod
    def delete_action_item(self, action_item_id: str, user_id: str) -> bool:
        """Delete an action item after verifying ownership through its parent call."""
        pass

    @abstractmethod
    def get_recording_signed_url(self, call_id: str, user_id: str, expires_in: int = 900) -> Dict[str, Any]:
        """Generate a short-lived signed URL for the call's private audio recording."""
        pass

    @abstractmethod
    def upload_recording(
        self, call_id: str, user_id: str, wav_bytes: bytes, duration_seconds: int
    ) -> Optional[str]:
        """Uploads a WAV audio recording to Supabase Storage and updates the call record."""
        pass


class InMemoryDatabaseService(DatabaseService):
    """
    In-memory database implementation that mirrors the PostgreSQL schema and constraints
    defined in 20261018160800_initial_schema.sql. Used for robust, hermetic testing without
    requiring live database or network connectivity.
    """

    def __init__(self):
        self.calls: Dict[str, Dict[str, Any]] = {}
        self.transcripts: Dict[str, Dict[str, Any]] = {}
        self.action_items: Dict[str, Dict[str, Any]] = {}
        # Simulated storage bucket mapping path -> {size_bytes, content}
        self.storage: Dict[str, Dict[str, Any]] = {}

    def create_call(self, user_id: str, data: SimulateCallRequest) -> Dict[str, Any]:
        call_id = str(uuid.uuid4())
        now = datetime.now(timezone.utc)
        record = {
            "id": call_id,
            "user_id": user_id,
            "caller_name": data.caller_name,
            "caller_number": data.caller_number,
            "status": "connecting",
            "call_type": "simulated",
            "caller_reason": None,
            "summary": None,
            "urgency": "low",
            "urgency_explanation": None,
            "uncertainties": None,
            "key_facts": [],
            "is_important": False,
            "duration_seconds": 0,
            "recording_path": None,
            "recording_duration_seconds": None,
            "recording_size_bytes": None,
            "created_at": now,
            "started_at": None,
            "ended_at": None,
        }
        self.calls[call_id] = record
        return copy.deepcopy(record)

    def list_calls(
        self,
        user_id: str,
        limit: int = 20,
        offset: int = 0,
        status: Optional[str] = None,
        search: Optional[str] = None,
        important_only: Optional[bool] = None,
    ) -> Tuple[List[Dict[str, Any]], int]:
        user_calls = [c for c in self.calls.values() if c["user_id"] == user_id]

        if status:
            user_calls = [c for c in user_calls if c["status"] == status]
        if important_only is not None:
            user_calls = [c for c in user_calls if c["is_important"] == important_only]
        if search:
            q = search.lower()
            user_calls = [
                c
                for c in user_calls
                if (c.get("caller_name") and q in c["caller_name"].lower())
                or (c.get("caller_number") and q in c["caller_number"].lower())
                or (c.get("summary") and q in c["summary"].lower())
                or (c.get("caller_reason") and q in c["caller_reason"].lower())
            ]

        # Newest first
        user_calls.sort(key=lambda c: c["created_at"], reverse=True)
        total = len(user_calls)
        paginated = user_calls[offset : offset + limit]
        return [copy.deepcopy(c) for c in paginated], total

    def get_call(self, call_id: str, user_id: str) -> Optional[Dict[str, Any]]:
        call = self.calls.get(call_id)
        if not call or call["user_id"] != user_id:
            return None
        return copy.deepcopy(call)

    def update_call(self, call_id: str, user_id: str, updates: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        call = self.calls.get(call_id)
        if not call or call["user_id"] != user_id:
            return None

        for k, v in updates.items():
            if v is not None:
                call[k] = v

        return copy.deepcopy(call)

    def delete_call(self, call_id: str, user_id: str) -> bool:
        call = self.calls.get(call_id)
        if not call or call["user_id"] != user_id:
            return False

        # 1. Clean up audio recording if present
        rec_path = call.get("recording_path")
        if rec_path:
            try:
                self.storage.pop(rec_path, None)
            except Exception as e:
                logger.warning("Failed to remove storage object %s during call deletion: %s", rec_path, e)

        # 2. Foreign-key cascade: remove child transcripts and action items
        t_keys_to_del = [tid for tid, t in self.transcripts.items() if t["call_id"] == call_id]
        for tid in t_keys_to_del:
            self.transcripts.pop(tid, None)

        a_keys_to_del = [aid for aid, a in self.action_items.items() if a["call_id"] == call_id]
        for aid in a_keys_to_del:
            self.action_items.pop(aid, None)

        # 3. Delete call row
        self.calls.pop(call_id, None)
        return True

    def list_transcripts(self, call_id: str, user_id: str) -> List[Dict[str, Any]]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        matching = [t for t in self.transcripts.values() if t["call_id"] == call_id]
        matching.sort(key=lambda t: t["sequence_order"])
        return [copy.deepcopy(t) for t in matching]

    def add_transcript(self, call_id: str, user_id: str, transcript: CreateTranscriptRequest) -> Dict[str, Any]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        # Check unique constraint (call_id, sequence_order)
        existing = [
            t
            for t in self.transcripts.values()
            if t["call_id"] == call_id and t["sequence_order"] == transcript.sequence_order
        ]

        if existing:
            first = existing[0]
            # Idempotent retry check: if speaker and text match, return existing entry safely
            if first["speaker"] == transcript.speaker and first["text"] == transcript.text:
                return copy.deepcopy(first)
            raise ConflictException(
                f"Transcript entry with sequence_order {transcript.sequence_order} already exists for this call."
            )

        transcript_id = str(uuid.uuid4())
        record = {
            "id": transcript_id,
            "call_id": call_id,
            "speaker": transcript.speaker,
            "text": transcript.text,
            "timestamp_ms": transcript.timestamp_ms,
            "sequence_order": transcript.sequence_order,
            "created_at": datetime.now(timezone.utc),
        }
        self.transcripts[transcript_id] = record
        return copy.deepcopy(record)

    def get_summary(self, call_id: str, user_id: str) -> Optional[Dict[str, Any]]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        # Do not fabricate summary if none exists
        if not call.get("summary"):
            return None

        return {
            "call_id": call_id,
            "summary": call["summary"],
            "caller_reason": call.get("caller_reason"),
            "urgency": call.get("urgency", "low"),
            "urgency_explanation": call.get("urgency_explanation"),
            "key_facts": call.get("key_facts", []),
            "uncertainties": call.get("uncertainties"),
            "duration_seconds": call.get("duration_seconds", 0),
        }

    def update_summary(self, call_id: str, user_id: str, summary_data: UpdateSummaryRequest) -> Dict[str, Any]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        updates = {
            "summary": summary_data.summary,
            "caller_reason": summary_data.caller_reason,
            "urgency": summary_data.urgency,
            "urgency_explanation": summary_data.urgency_explanation,
            "key_facts": summary_data.key_facts,
            "uncertainties": summary_data.uncertainties,
        }
        self.update_call(call_id, user_id, updates)
        return {
            "call_id": call_id,
            **updates,
            "duration_seconds": call.get("duration_seconds", 0),
        }

    def list_action_items(self, call_id: str, user_id: str) -> List[Dict[str, Any]]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        items = [a for a in self.action_items.values() if a["call_id"] == call_id]
        items.sort(key=lambda a: a["created_at"])
        return [copy.deepcopy(a) for a in items]

    def create_action_item(self, call_id: str, user_id: str, item: CreateActionItemRequest) -> Dict[str, Any]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        action_id = str(uuid.uuid4())
        record = {
            "id": action_id,
            "call_id": call_id,
            "task": item.task,
            "due_date": item.due_date,
            "completed": False,
            "completed_at": None,
            "created_at": datetime.now(timezone.utc),
        }
        self.action_items[action_id] = record
        return copy.deepcopy(record)

    def update_action_item(
        self, action_item_id: str, user_id: str, updates: Dict[str, Any]
    ) -> Optional[Dict[str, Any]]:
        item = self.action_items.get(action_item_id)
        if not item:
            return None

        # Verify parent call ownership
        call = self.calls.get(item["call_id"])
        if not call or call["user_id"] != user_id:
            return None

        if "task" in updates and updates["task"] is not None:
            item["task"] = updates["task"]
        if "due_date" in updates:
            item["due_date"] = updates["due_date"]
        if "completed" in updates and updates["completed"] is not None:
            item["completed"] = updates["completed"]
            if updates["completed"]:
                item["completed_at"] = datetime.now(timezone.utc)
            else:
                item["completed_at"] = None

        return copy.deepcopy(item)

    def delete_action_item(self, action_item_id: str, user_id: str) -> bool:
        item = self.action_items.get(action_item_id)
        if not item:
            return False

        # Verify parent call ownership
        call = self.calls.get(item["call_id"])
        if not call or call["user_id"] != user_id:
            return False

        self.action_items.pop(action_item_id, None)
        return True

    def get_recording_signed_url(self, call_id: str, user_id: str, expires_in: int = 900) -> Dict[str, Any]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        recording_path = call.get("recording_path")
        if not recording_path:
            raise NotFoundException("No recording found for this call")

        # Verify path matches user isolation folder: {user_id}/{call_id}.wav
        expected_prefix = f"{user_id}/"
        if not recording_path.startswith(expected_prefix):
            raise ForbiddenException("Recording path violates user isolation boundary")

        # Verify storage file existence
        if recording_path not in self.storage:
            raise NotFoundException("Recording audio file not found in storage bucket")

        now = datetime.now(timezone.utc)
        expires_at = datetime.fromtimestamp(now.timestamp() + expires_in, timezone.utc)
        signed_url = (
            f"https://{settings.SUPABASE_URL.replace('https://', '')}/storage/v1/object/sign/"
            f"{settings.SUPABASE_STORAGE_BUCKET}/{recording_path}?token=mock_signed_token_{call_id[:8]}"
        )

        return {
            "call_id": call_id,
            "signed_url": signed_url,
            "expires_at": expires_at,
            "recording_path": recording_path,
            "file_size_bytes": self.storage[recording_path].get("size_bytes", call.get("recording_size_bytes")),
        }

    def upload_recording(
        self, call_id: str, user_id: str, wav_bytes: bytes, duration_seconds: int
    ) -> Optional[str]:
        call = self.get_call(call_id, user_id)
        if not call:
            return None
        path = f"{user_id}/{call_id}.wav"
        self.storage[path] = {
            "data": wav_bytes,
            "size_bytes": len(wav_bytes),
            "content_type": "audio/wav",
        }
        self.update_call(call_id, user_id, {
            "recording_path": path,
            "recording_duration_seconds": duration_seconds,
            "recording_size_bytes": len(wav_bytes),
            "has_recording": True,
        })
        return path


class SupabaseDatabaseService(DatabaseService):
    """
    Live Supabase PostgreSQL and Storage persistence layer.
    Uses supabase-py client with strictly enforced caller-level ownership.
    Even with service-role credentials, user_id is checked on every query.
    """

    def __init__(self, supabase_client: Any = None):
        if supabase_client is not None:
            self._client = supabase_client
        else:
            from supabase import create_client

            # Prefer service-role key for backend operations, fallback to anon
            key = settings.SUPABASE_SERVICE_ROLE_KEY or settings.SUPABASE_ANON_KEY
            self._client = create_client(settings.SUPABASE_URL, key)

    def _safe_execute(self, query: Any) -> Any:
        try:
            return query.execute()
        except Exception as e:
            logger.error("Supabase query execution failed: %s", e)
            raise AppException(
                message="Database operation failed. Please try again.",
                code="DATABASE_ERROR",
                status_code=500,
            )

    def create_call(self, user_id: str, data: SimulateCallRequest) -> Dict[str, Any]:
        call_id = str(uuid.uuid4())
        now_iso = datetime.now(timezone.utc).isoformat()
        payload = {
            "id": call_id,
            "user_id": user_id,
            "caller_name": data.caller_name,
            "caller_number": data.caller_number,
            "status": "connecting",
            "call_type": "simulated",
            "caller_reason": None,
            "summary": None,
            "urgency": "low",
            "urgency_explanation": None,
            "uncertainties": None,
            "key_facts": [],
            "is_important": False,
            "duration_seconds": 0,
            "created_at": now_iso,
        }
        res = self._safe_execute(self._client.table("calls").insert(payload))
        if res.data and len(res.data) > 0:
            return res.data[0]
        return payload

    def list_calls(
        self,
        user_id: str,
        limit: int = 20,
        offset: int = 0,
        status: Optional[str] = None,
        search: Optional[str] = None,
        important_only: Optional[bool] = None,
    ) -> Tuple[List[Dict[str, Any]], int]:
        q = (
            self._client.table("calls")
            .select("*", count="exact")
            .eq("user_id", user_id)
            .order("created_at", desc=True)
        )

        if status:
            q = q.eq("status", status)
        if important_only is not None:
            q = q.eq("is_important", important_only)
        if search:
            escaped = search.replace("%", "").replace(",", "")
            q = q.or_(f"caller_name.ilike.%{escaped}%,caller_number.ilike.%{escaped}%,summary.ilike.%{escaped}%")

        q = q.range(offset, offset + limit - 1)
        res = self._safe_execute(q)
        total = res.count if res.count is not None else len(res.data or [])
        return res.data or [], total

    def get_call(self, call_id: str, user_id: str) -> Optional[Dict[str, Any]]:
        q = (
            self._client.table("calls")
            .select("*")
            .eq("id", call_id)
            .eq("user_id", user_id)
            .maybe_single()
        )
        res = self._safe_execute(q)
        return res.data if res.data else None

    def update_call(self, call_id: str, user_id: str, updates: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        # Strip None fields and serialize datetimes
        filtered: Dict[str, Any] = {}
        for k, v in updates.items():
            if v is not None:
                if isinstance(v, datetime):
                    filtered[k] = v.isoformat()
                else:
                    filtered[k] = v

        if not filtered:
            return self.get_call(call_id, user_id)

        q = (
            self._client.table("calls")
            .update(filtered)
            .eq("id", call_id)
            .eq("user_id", user_id)
        )
        res = self._safe_execute(q)
        return res.data[0] if (res.data and len(res.data) > 0) else None

    def delete_call(self, call_id: str, user_id: str) -> bool:
        call = self.get_call(call_id, user_id)
        if not call:
            return False

        # Clean up storage file if exists
        rec_path = call.get("recording_path")
        if rec_path:
            try:
                self._client.storage.from_(settings.SUPABASE_STORAGE_BUCKET).remove([rec_path])
            except Exception as e:
                logger.warning("Failed to remove storage object %s during call deletion: %s", rec_path, e)

        # Deleting from calls cascades to call_transcripts and action_items via DB FK constraints
        q = self._client.table("calls").delete().eq("id", call_id).eq("user_id", user_id)
        self._safe_execute(q)
        return True

    def list_transcripts(self, call_id: str, user_id: str) -> List[Dict[str, Any]]:
        # Enforce parent call ownership
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        q = (
            self._client.table("call_transcripts")
            .select("*")
            .eq("call_id", call_id)
            .order("sequence_order", desc=False)
        )
        res = self._safe_execute(q)
        return res.data or []

    def add_transcript(self, call_id: str, user_id: str, transcript: CreateTranscriptRequest) -> Dict[str, Any]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        # Check existing sequence_order for idempotency
        check_q = (
            self._client.table("call_transcripts")
            .select("*")
            .eq("call_id", call_id)
            .eq("sequence_order", transcript.sequence_order)
            .maybe_single()
        )
        res_check = self._safe_execute(check_q)
        existing = res_check.data if (res_check and getattr(res_check, "data", None)) else None
        if existing:
            if existing["speaker"] == transcript.speaker and existing["text"] == transcript.text:
                return existing
            raise ConflictException(
                f"Transcript entry with sequence_order {transcript.sequence_order} already exists for this call."
            )

        new_id = str(uuid.uuid4())
        payload = {
            "id": new_id,
            "call_id": call_id,
            "speaker": transcript.speaker,
            "text": transcript.text,
            "timestamp_ms": transcript.timestamp_ms,
            "sequence_order": transcript.sequence_order,
            "created_at": datetime.now(timezone.utc).isoformat(),
        }
        res = self._safe_execute(self._client.table("call_transcripts").insert(payload))
        return res.data[0] if (res.data and len(res.data) > 0) else payload

    def get_summary(self, call_id: str, user_id: str) -> Optional[Dict[str, Any]]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        if not call.get("summary"):
            return None

        return {
            "call_id": call_id,
            "summary": call["summary"],
            "caller_reason": call.get("caller_reason"),
            "urgency": call.get("urgency", "low"),
            "urgency_explanation": call.get("urgency_explanation"),
            "key_facts": call.get("key_facts", []),
            "uncertainties": call.get("uncertainties"),
            "duration_seconds": call.get("duration_seconds", 0),
        }

    def update_summary(self, call_id: str, user_id: str, summary_data: UpdateSummaryRequest) -> Dict[str, Any]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        updates = {
            "summary": summary_data.summary,
            "caller_reason": summary_data.caller_reason,
            "urgency": summary_data.urgency,
            "urgency_explanation": summary_data.urgency_explanation,
            "key_facts": summary_data.key_facts,
            "uncertainties": summary_data.uncertainties,
        }
        self.update_call(call_id, user_id, updates)
        return {
            "call_id": call_id,
            **updates,
            "duration_seconds": call.get("duration_seconds", 0),
        }

    def list_action_items(self, call_id: str, user_id: str) -> List[Dict[str, Any]]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        q = (
            self._client.table("action_items")
            .select("*")
            .eq("call_id", call_id)
            .order("created_at", desc=False)
        )
        res = self._safe_execute(q)
        return res.data or []

    def create_action_item(self, call_id: str, user_id: str, item: CreateActionItemRequest) -> Dict[str, Any]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        item_id = str(uuid.uuid4())
        payload = {
            "id": item_id,
            "call_id": call_id,
            "task": item.task,
            "due_date": item.due_date.isoformat() if item.due_date else None,
            "completed": False,
            "completed_at": None,
            "created_at": datetime.now(timezone.utc).isoformat(),
        }
        res = self._safe_execute(self._client.table("action_items").insert(payload))
        return res.data[0] if (res.data and len(res.data) > 0) else payload

    def update_action_item(
        self, action_item_id: str, user_id: str, updates: Dict[str, Any]
    ) -> Optional[Dict[str, Any]]:
        # 1. Fetch item
        item_q = (
            self._client.table("action_items")
            .select("*")
            .eq("id", action_item_id)
            .maybe_single()
        )
        item_res = self._safe_execute(item_q)
        item = item_res.data if (item_res and getattr(item_res, "data", None)) else None
        if not item:
            return None

        # 2. Verify parent call ownership
        call = self.get_call(item["call_id"], user_id)
        if not call:
            return None

        payload: Dict[str, Any] = {}
        if "task" in updates and updates["task"] is not None:
            payload["task"] = updates["task"]
        if "due_date" in updates:
            payload["due_date"] = updates["due_date"].isoformat() if isinstance(updates["due_date"], datetime) else updates["due_date"]
        if "completed" in updates and updates["completed"] is not None:
            payload["completed"] = updates["completed"]
            payload["completed_at"] = (
                datetime.now(timezone.utc).isoformat() if updates["completed"] else None
            )

        if not payload:
            return item

        q = (
            self._client.table("action_items")
            .update(payload)
            .eq("id", action_item_id)
        )
        res = self._safe_execute(q)
        return res.data[0] if (res and getattr(res, "data", None) and len(res.data) > 0) else None

    def delete_action_item(self, action_item_id: str, user_id: str) -> bool:
        item_q = (
            self._client.table("action_items")
            .select("*")
            .eq("id", action_item_id)
            .maybe_single()
        )
        item_res = self._safe_execute(item_q)
        item = item_res.data if (item_res and getattr(item_res, "data", None)) else None
        if not item:
            return False

        # Verify parent call ownership
        call = self.get_call(item["call_id"], user_id)
        if not call:
            return False

        q = self._client.table("action_items").delete().eq("id", action_item_id)
        self._safe_execute(q)
        return True

    def get_recording_signed_url(self, call_id: str, user_id: str, expires_in: int = 900) -> Dict[str, Any]:
        call = self.get_call(call_id, user_id)
        if not call:
            raise NotFoundException("Call not found")

        rec_path = call.get("recording_path")
        if not rec_path:
            raise NotFoundException("No recording found for this call")

        # Isolation verification
        if not rec_path.startswith(f"{user_id}/"):
            raise ForbiddenException("Recording path violates user isolation boundary")

        try:
            # Generate signed URL from Supabase Storage
            res = self._client.storage.from_(settings.SUPABASE_STORAGE_BUCKET).create_signed_url(
                rec_path, expires_in
            )
            # Response may be dict or SignedUrlResponse object
            signed_url = None
            if hasattr(res, "signedURL") and res.signedURL:
                signed_url = res.signedURL
            elif hasattr(res, "signedUrl") and res.signedUrl:
                signed_url = res.signedUrl
            elif isinstance(res, dict):
                signed_url = res.get("signedURL") or res.get("signedUrl")

            if not signed_url:
                raise NotFoundException("Recording audio file not found in storage bucket")

            now = datetime.now(timezone.utc)
            expires_at = datetime.fromtimestamp(now.timestamp() + expires_in, timezone.utc)

            return {
                "call_id": call_id,
                "signed_url": signed_url,
                "expires_at": expires_at,
                "recording_path": rec_path,
                "file_size_bytes": call.get("recording_size_bytes"),
            }
        except NotFoundException:
            raise
        except Exception as e:
            logger.error("Storage error creating signed URL for %s: %s", rec_path, e)
            raise AppException(
                message="Failed to generate secure recording playback URL.",
                code="STORAGE_ERROR",
                status_code=500,
            )

    def upload_recording(
        self, call_id: str, user_id: str, wav_bytes: bytes, duration_seconds: int
    ) -> Optional[str]:
        call = self.get_call(call_id, user_id)
        if not call:
            logger.warning("Cannot upload recording: call %s not found for user %s", call_id, user_id)
            return None
        path = f"{user_id}/{call_id}.wav"
        try:
            self._client.storage.from_(settings.SUPABASE_STORAGE_BUCKET).upload(
                path=path,
                file=wav_bytes,
                file_options={"content-type": "audio/wav", "upsert": "true"},
            )
            self.update_call(call_id, user_id, {
                "recording_path": path,
                "recording_duration_seconds": duration_seconds,
                "recording_size_bytes": len(wav_bytes),
                "has_recording": True,
            })
            logger.info("Successfully uploaded recording for call %s (%d bytes)", call_id, len(wav_bytes))
            return path
        except Exception as e:
            logger.error("Failed to upload recording for call %s: %s", call_id, e)
            return None


# Default singleton provider
_default_db_service: Optional[DatabaseService] = None


def get_database_service() -> DatabaseService:
    """
    Returns the configured DatabaseService instance.
    In development or production with real Supabase credentials, returns SupabaseDatabaseService.
    If SUPABASE_URL contains placeholder, defaults to InMemoryDatabaseService for safety.
    """
    global _default_db_service
    if _default_db_service is None:
        if "placeholder" in settings.SUPABASE_URL.lower() or not settings.SUPABASE_SERVICE_ROLE_KEY:
            _default_db_service = InMemoryDatabaseService()
        else:
            try:
                _default_db_service = SupabaseDatabaseService()
            except Exception as e:
                logger.warning("Could not initialize Supabase client (%s). Falling back to InMemory.", e)
                _default_db_service = InMemoryDatabaseService()
    return _default_db_service


def set_database_service(service: DatabaseService) -> None:
    """Explicitly override the database service (useful for testing fixtures)."""
    global _default_db_service
    _default_db_service = service
