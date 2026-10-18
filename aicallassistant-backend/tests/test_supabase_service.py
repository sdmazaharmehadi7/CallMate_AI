import uuid
from unittest.mock import MagicMock
import pytest

from app.core.errors import AppException, ForbiddenException, NotFoundException
from app.models.schemas import SimulateCallRequest
from app.services.database import SupabaseDatabaseService


def test_supabase_service_create_call_enforces_user_id():
    mock_client = MagicMock()
    mock_table = MagicMock()
    mock_insert = MagicMock()
    mock_client.table.return_value = mock_table
    mock_table.insert.return_value = mock_insert
    mock_insert.execute.return_value = MagicMock(data=[{"id": "call-1", "user_id": "user-123"}])

    service = SupabaseDatabaseService(supabase_client=mock_client)
    req = SimulateCallRequest(caller_name="Caller A", caller_number="+919876543210")
    res = service.create_call(user_id="user-123", data=req)

    mock_client.table.assert_called_with("calls")
    call_args = mock_table.insert.call_args[0][0]
    assert call_args["user_id"] == "user-123"
    assert call_args["caller_name"] == "Caller A"
    assert res["id"] == "call-1"


def test_supabase_service_query_error_wrapped_safely():
    mock_client = MagicMock()
    mock_table = MagicMock()
    mock_client.table.return_value = mock_table
    # Simulate database connection / execution error at query execution time
    mock_query = MagicMock()
    mock_query.execute.side_effect = RuntimeError("Internal PostgreSQL connection timeout: host=db.internal:5432")
    mock_table.select.return_value.eq.return_value.eq.return_value.maybe_single.return_value = mock_query

    service = SupabaseDatabaseService(supabase_client=mock_client)
    with pytest.raises(AppException) as exc_info:
        service.get_call("call-1", "user-123")

    # Verify internal DB connection strings and SQL details are not leaked in message
    assert exc_info.value.code == "DATABASE_ERROR"
    assert "db.internal" not in exc_info.value.message
    assert exc_info.value.status_code == 500



def test_supabase_service_recording_isolation_check():
    mock_client = MagicMock()
    service = SupabaseDatabaseService(supabase_client=mock_client)

    # Mock get_call returning a recording that belongs to another user folder
    service.get_call = MagicMock(return_value={
        "id": "call-1",
        "user_id": "user-a",
        "recording_path": "user-b/call-1.wav",  # Malformed / cross-user path!
    })

    with pytest.raises(ForbiddenException) as exc_info:
        service.get_recording_signed_url("call-1", "user-a")

    assert "isolation" in exc_info.value.message.lower()
