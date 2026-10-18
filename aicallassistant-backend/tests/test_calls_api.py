import uuid
import pytest
from tests.conftest import USER_A_ID, USER_B_ID


def test_simulate_call_success(client, user_a_headers):
    payload = {
        "caller_name": "Dr. Sarah Smith",
        "caller_number": "+919876543210",
        "voice_persona": "Puck",
        "language": "en-IN",
    }
    response = client.post("/api/v1/calls/simulate", json=payload, headers=user_a_headers)
    assert response.status_code == 201
    data = response.json()
    assert "call_id" in data
    assert data["status"] == "connecting"
    assert "/ws/calls/" in data["ws_url"]
    assert "created_at" in data


def test_simulate_call_unauthorized(client):
    payload = {"caller_name": "Test", "caller_number": "+1234567890"}
    response = client.post("/api/v1/calls/simulate", json=payload)
    assert response.status_code == 401
    assert response.json()["error"]["code"] == "UNAUTHORIZED"


def test_list_calls_pagination_and_ordering(client, user_a_headers):
    # Empty initially
    res0 = client.get("/api/v1/calls", headers=user_a_headers)
    assert res0.status_code == 200
    assert res0.json()["calls"] == []
    assert res0.json()["total"] == 0

    # Create 3 calls
    ids = []
    for i in range(3):
        r = client.post(
            "/api/v1/calls/simulate",
            json={"caller_name": f"Caller {i}", "caller_number": f"+91987650000{i}"},
            headers=user_a_headers,
        )
        assert r.status_code == 201
        ids.append(r.json()["call_id"])

    # Page 1: limit 2, offset 0 -> newest first (Caller 2, then Caller 1)
    res_p1 = client.get("/api/v1/calls?limit=2&offset=0", headers=user_a_headers)
    assert res_p1.status_code == 200
    data_p1 = res_p1.json()
    assert data_p1["total"] == 3
    assert len(data_p1["calls"]) == 2
    assert data_p1["calls"][0]["id"] == ids[2]
    assert data_p1["calls"][1]["id"] == ids[1]

    # Page 2: limit 2, offset 2 -> Caller 0
    res_p2 = client.get("/api/v1/calls?limit=2&offset=2", headers=user_a_headers)
    assert res_p2.status_code == 200
    data_p2 = res_p2.json()
    assert len(data_p2["calls"]) == 1
    assert data_p2["calls"][0]["id"] == ids[0]


def test_list_calls_filtering(client, user_a_headers, test_db):
    # Create two calls
    r1 = client.post("/api/v1/calls/simulate", json={"caller_name": "Amazon Courier", "caller_number": "+919999999999"}, headers=user_a_headers)
    call1_id = r1.json()["call_id"]
    r2 = client.post("/api/v1/calls/simulate", json={"caller_name": "Bank Manager", "caller_number": "+918888888888"}, headers=user_a_headers)
    call2_id = r2.json()["call_id"]

    # Mark call1 as important and completed
    client.patch(f"/api/v1/calls/{call1_id}", json={"is_important": True, "status": "active"}, headers=user_a_headers)
    client.patch(f"/api/v1/calls/{call1_id}", json={"status": "completed"}, headers=user_a_headers)

    # Filter by status=completed
    res_completed = client.get("/api/v1/calls?status=completed", headers=user_a_headers)
    assert res_completed.status_code == 200
    assert len(res_completed.json()["calls"]) == 1
    assert res_completed.json()["calls"][0]["id"] == call1_id

    # Filter by important_only=True
    res_important = client.get("/api/v1/calls?important_only=true", headers=user_a_headers)
    assert res_important.status_code == 200
    assert len(res_important.json()["calls"]) == 1
    assert res_important.json()["calls"][0]["id"] == call1_id

    # Search by keyword
    res_search = client.get("/api/v1/calls?search=Amazon", headers=user_a_headers)
    assert res_search.status_code == 200
    assert len(res_search.json()["calls"]) == 1
    assert res_search.json()["calls"][0]["id"] == call1_id


def test_get_call_details(client, user_a_headers):
    # Create call
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Alice"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    # Add a transcript
    t_res = client.post(
        f"/api/v1/calls/{call_id}/transcripts",
        json={"speaker": "assistant", "text": "Hello, how can I help you?", "timestamp_ms": 100, "sequence_order": 0},
        headers=user_a_headers,
    )
    assert t_res.status_code == 201

    # Add an action item
    a_res = client.post(
        f"/api/v1/calls/{call_id}/action-items",
        json={"task": "Follow up with Alice"},
        headers=user_a_headers,
    )
    assert a_res.status_code == 201

    # Fetch details
    detail_res = client.get(f"/api/v1/calls/{call_id}", headers=user_a_headers)
    assert detail_res.status_code == 200
    detail = detail_res.json()
    assert detail["id"] == call_id
    assert detail["caller_name"] == "Alice"
    assert len(detail["transcripts"]) == 1
    assert detail["transcripts"][0]["text"] == "Hello, how can I help you?"
    assert len(detail["action_items"]) == 1
    assert detail["action_items"][0]["task"] == "Follow up with Alice"


def test_get_call_invalid_uuid(client, user_a_headers):
    response = client.get("/api/v1/calls/not-a-valid-uuid", headers=user_a_headers)
    assert response.status_code == 400
    assert response.json()["error"]["code"] == "BAD_REQUEST"


def test_get_call_cross_user_denied(client, user_a_headers, user_b_headers):
    # User A creates a call
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Private Call"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    # User B requests User A's call
    res = client.get(f"/api/v1/calls/{call_id}", headers=user_b_headers)
    assert res.status_code == 404
    assert res.json()["error"]["code"] == "NOT_FOUND"


def test_patch_call_permitted_fields(client, user_a_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Client"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    patch_res = client.patch(
        f"/api/v1/calls/{call_id}",
        json={
            "is_important": True,
            "caller_reason": "Contract discussion",
            "urgency": "high",
            "urgency_explanation": "Client requested an answer today",
            "key_facts": ["Project deadline is Friday", "Budget approved"],
            "uncertainties": "Pricing model not yet locked",
        },
        headers=user_a_headers,
    )
    assert patch_res.status_code == 200
    data = patch_res.json()
    assert data["is_important"] is True
    assert data["caller_reason"] == "Contract discussion"
    assert data["urgency"] == "high"
    assert len(data["key_facts"]) == 2
    assert data["uncertainties"] == "Pricing model not yet locked"


def test_patch_call_status_transitions(client, user_a_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Client"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    # Valid: connecting -> active
    r_active = client.patch(f"/api/v1/calls/{call_id}", json={"status": "active"}, headers=user_a_headers)
    assert r_active.status_code == 200
    assert r_active.json()["status"] == "active"
    assert r_active.json()["started_at"] is not None

    # Valid: active -> completed
    r_completed = client.patch(f"/api/v1/calls/{call_id}", json={"status": "completed"}, headers=user_a_headers)
    assert r_completed.status_code == 200
    assert r_completed.json()["status"] == "completed"
    assert r_completed.json()["ended_at"] is not None

    # Invalid: completed -> connecting
    r_invalid = client.patch(f"/api/v1/calls/{call_id}", json={"status": "connecting"}, headers=user_a_headers)
    assert r_invalid.status_code == 400
    assert r_invalid.json()["error"]["code"] == "BAD_REQUEST"


def test_patch_call_cross_user_denied(client, user_a_headers, user_b_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "User A call"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    res = client.patch(f"/api/v1/calls/{call_id}", json={"is_important": True}, headers=user_b_headers)
    assert res.status_code == 404


def test_delete_call_and_cascade(client, user_a_headers, test_db):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "To be deleted"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    # Add child transcript and action item
    client.post(
        f"/api/v1/calls/{call_id}/transcripts",
        json={"speaker": "caller", "text": "Goodbye", "sequence_order": 0},
        headers=user_a_headers,
    )
    client.post(
        f"/api/v1/calls/{call_id}/action-items",
        json={"task": "Delete test item"},
        headers=user_a_headers,
    )

    # Attach simulated recording
    rec_path = f"{USER_A_ID}/{call_id}.wav"
    test_db.calls[call_id]["recording_path"] = rec_path
    test_db.storage[rec_path] = {"size_bytes": 4096}

    # Delete call
    del_res = client.delete(f"/api/v1/calls/{call_id}", headers=user_a_headers)
    assert del_res.status_code == 200
    assert del_res.json()["deleted"] is True
    assert del_res.json()["id"] == call_id

    # Verify call is gone
    get_res = client.get(f"/api/v1/calls/{call_id}", headers=user_a_headers)
    assert get_res.status_code == 404

    # Verify storage file was deleted
    assert rec_path not in test_db.storage

    # Verify transcripts and action items were cascaded
    assert not any(t["call_id"] == call_id for t in test_db.transcripts.values())
    assert not any(a["call_id"] == call_id for a in test_db.action_items.values())


def test_delete_call_cross_user_denied(client, user_a_headers, user_b_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Owner A"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    res = client.delete(f"/api/v1/calls/{call_id}", headers=user_b_headers)
    assert res.status_code == 404


def test_transcripts_duplicate_and_idempotency(client, user_a_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Transcript Test"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    # Turn 0
    t0 = client.post(
        f"/api/v1/calls/{call_id}/transcripts",
        json={"speaker": "assistant", "text": "Hello there", "sequence_order": 0, "timestamp_ms": 100},
        headers=user_a_headers,
    )
    assert t0.status_code == 201
    item0_id = t0.json()["id"]

    # Turn 1
    t1 = client.post(
        f"/api/v1/calls/{call_id}/transcripts",
        json={"speaker": "caller", "text": "Hi, I need assistance", "sequence_order": 1, "timestamp_ms": 1500},
        headers=user_a_headers,
    )
    assert t1.status_code == 201

    # Idempotent retry of Turn 0 with identical speaker & text -> returns existing transcript item
    t0_retry = client.post(
        f"/api/v1/calls/{call_id}/transcripts",
        json={"speaker": "assistant", "text": "Hello there", "sequence_order": 0, "timestamp_ms": 100},
        headers=user_a_headers,
    )
    assert t0_retry.status_code == 201 or t0_retry.status_code == 200
    assert t0_retry.json()["id"] == item0_id

    # Conflict retry of Turn 0 with DIFFERENT text -> returns 409 Conflict
    t0_conflict = client.post(
        f"/api/v1/calls/{call_id}/transcripts",
        json={"speaker": "assistant", "text": "Completely different text", "sequence_order": 0, "timestamp_ms": 100},
        headers=user_a_headers,
    )
    assert t0_conflict.status_code == 409
    assert t0_conflict.json()["error"]["code"] == "CONFLICT"


def test_transcripts_cross_user_denied(client, user_a_headers, user_b_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Call A"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    # User B tries to read transcripts
    get_res = client.get(f"/api/v1/calls/{call_id}/transcripts", headers=user_b_headers)
    assert get_res.status_code == 404

    # User B tries to insert transcript
    post_res = client.post(
        f"/api/v1/calls/{call_id}/transcripts",
        json={"speaker": "caller", "text": "Intruder", "sequence_order": 0},
        headers=user_b_headers,
    )
    assert post_res.status_code == 404


def test_summary_endpoints(client, user_a_headers, user_b_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Summary Call"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    # Before summary is created -> 404 Not Found (no fabrication)
    get_none = client.get(f"/api/v1/calls/{call_id}/summary", headers=user_a_headers)
    assert get_none.status_code == 404
    assert get_none.json()["error"]["code"] == "NOT_FOUND"

    # Put summary
    put_res = client.put(
        f"/api/v1/calls/{call_id}/summary",
        json={
            "summary": "Courier arrived with passport delivery. User gave OTP over call.",
            "caller_reason": "Passport delivery verification",
            "urgency": "critical",
            "urgency_explanation": "Official government document delivered",
            "key_facts": ["Passport delivered", "OTP validated"],
            "uncertainties": "Neighbor signature not recorded",
        },
        headers=user_a_headers,
    )
    assert put_res.status_code == 200
    assert put_res.json()["urgency"] == "critical"

    # Subsequent GET returns stored summary
    get_res = client.get(f"/api/v1/calls/{call_id}/summary", headers=user_a_headers)
    assert get_res.status_code == 200
    assert get_res.json()["summary"] == "Courier arrived with passport delivery. User gave OTP over call."

    # User B cannot read or put summary
    assert client.get(f"/api/v1/calls/{call_id}/summary", headers=user_b_headers).status_code == 404
    assert client.put(f"/api/v1/calls/{call_id}/summary", json={"summary": "Fake"}, headers=user_b_headers).status_code == 404


def test_recording_url_endpoints(client, user_a_headers, user_b_headers, test_db):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Audio Call"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    # 1. No recording reference on call -> 404
    res_no_ref = client.get(f"/api/v1/calls/{call_id}/recording-url", headers=user_a_headers)
    assert res_no_ref.status_code == 404

    # 2. Reference exists but file missing from storage -> 404
    rec_path = f"{USER_A_ID}/{call_id}.wav"
    test_db.calls[call_id]["recording_path"] = rec_path
    res_no_file = client.get(f"/api/v1/calls/{call_id}/recording-url", headers=user_a_headers)
    assert res_no_file.status_code == 404

    # 3. Reference and file present -> 200 Signed URL
    test_db.storage[rec_path] = {"size_bytes": 16384}
    res_ok = client.get(f"/api/v1/calls/{call_id}/recording-url", headers=user_a_headers)
    assert res_ok.status_code == 200
    data = res_ok.json()
    assert data["call_id"] == call_id
    assert "token=" in data["signed_url"]
    assert data["recording_path"] == rec_path
    assert data["file_size_bytes"] == 16384

    # 4. User B cannot access User A's recording URL -> 404
    res_b = client.get(f"/api/v1/calls/{call_id}/recording-url", headers=user_b_headers)
    assert res_b.status_code == 404
