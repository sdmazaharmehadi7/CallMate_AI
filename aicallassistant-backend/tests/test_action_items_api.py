import pytest
from tests.conftest import USER_A_ID, USER_B_ID


def test_create_and_list_action_items(client, user_a_headers):
    # Create parent call
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Task Creator"}, headers=user_a_headers)
    call_id = r.json()["call_id"]

    # Initial list is empty
    l0 = client.get(f"/api/v1/calls/{call_id}/action-items", headers=user_a_headers)
    assert l0.status_code == 200
    assert l0.json()["action_items"] == []
    assert l0.json()["total"] == 0

    # Create item
    payload = {"task": "Return missed call to Bank Manager"}
    c1 = client.post(f"/api/v1/calls/{call_id}/action-items", json=payload, headers=user_a_headers)
    assert c1.status_code == 201
    item_id = c1.json()["id"]
    assert c1.json()["task"] == "Return missed call to Bank Manager"
    assert c1.json()["completed"] is False
    assert c1.json()["completed_at"] is None

    # List items
    l1 = client.get(f"/api/v1/calls/{call_id}/action-items", headers=user_a_headers)
    assert l1.status_code == 200
    assert l1.json()["total"] == 1
    assert l1.json()["action_items"][0]["id"] == item_id


def test_patch_action_item_toggle_completed(client, user_a_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Task Creator"}, headers=user_a_headers)
    call_id = r.json()["call_id"]
    c1 = client.post(
        f"/api/v1/calls/{call_id}/action-items",
        json={"task": "Buy groceries"},
        headers=user_a_headers,
    )
    item_id = c1.json()["id"]

    # Mark complete
    p1 = client.patch(f"/api/v1/action-items/{item_id}", json={"completed": True}, headers=user_a_headers)
    assert p1.status_code == 200
    assert p1.json()["completed"] is True
    assert p1.json()["completed_at"] is not None

    # Mark incomplete
    p2 = client.patch(f"/api/v1/action-items/{item_id}", json={"completed": False}, headers=user_a_headers)
    assert p2.status_code == 200
    assert p2.json()["completed"] is False
    assert p2.json()["completed_at"] is None


def test_patch_action_item_update_task_text(client, user_a_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Task Creator"}, headers=user_a_headers)
    call_id = r.json()["call_id"]
    c1 = client.post(f"/api/v1/calls/{call_id}/action-items", json={"task": "Initial task"}, headers=user_a_headers)
    item_id = c1.json()["id"]

    p = client.patch(f"/api/v1/action-items/{item_id}", json={"task": "Updated task description"}, headers=user_a_headers)
    assert p.status_code == 200
    assert p.json()["task"] == "Updated task description"


def test_delete_action_item(client, user_a_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Task Creator"}, headers=user_a_headers)
    call_id = r.json()["call_id"]
    c1 = client.post(f"/api/v1/calls/{call_id}/action-items", json={"task": "Temporary item"}, headers=user_a_headers)
    item_id = c1.json()["id"]

    # Delete
    d = client.delete(f"/api/v1/action-items/{item_id}", headers=user_a_headers)
    assert d.status_code == 200
    assert d.json()["deleted"] is True
    assert d.json()["id"] == item_id

    # Verify not found on subsequent delete or patch
    assert client.delete(f"/api/v1/action-items/{item_id}", headers=user_a_headers).status_code == 404
    assert client.patch(f"/api/v1/action-items/{item_id}", json={"completed": True}, headers=user_a_headers).status_code == 404


def test_action_items_cross_user_denied(client, user_a_headers, user_b_headers):
    r = client.post("/api/v1/calls/simulate", json={"caller_name": "Task Creator"}, headers=user_a_headers)
    call_id = r.json()["call_id"]
    c1 = client.post(f"/api/v1/calls/{call_id}/action-items", json={"task": "Private task"}, headers=user_a_headers)
    item_id = c1.json()["id"]

    # User B cannot list or add items to User A's call
    assert client.get(f"/api/v1/calls/{call_id}/action-items", headers=user_b_headers).status_code == 404
    assert client.post(f"/api/v1/calls/{call_id}/action-items", json={"task": "Intruder task"}, headers=user_b_headers).status_code == 404

    # User B cannot patch or delete User A's action item
    assert client.patch(f"/api/v1/action-items/{item_id}", json={"completed": True}, headers=user_b_headers).status_code == 404
    assert client.delete(f"/api/v1/action-items/{item_id}", headers=user_b_headers).status_code == 404


def test_action_item_invalid_uuid(client, user_a_headers):
    assert client.patch("/api/v1/action-items/invalid-uuid", json={"completed": True}, headers=user_a_headers).status_code == 400
    assert client.delete("/api/v1/action-items/invalid-uuid", headers=user_a_headers).status_code == 400
