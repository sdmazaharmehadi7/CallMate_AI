def test_root_health_endpoint(client):
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["version"] == "0.1.0"
    assert "environment" in data
    assert "timestamp" in data
    assert "gemini_configured" in data


def test_v1_health_endpoint(client):
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"


def test_not_found_endpoint_returns_json_error(client):
    response = client.get("/api/v1/nonexistent-endpoint")
    assert response.status_code == 404
    data = response.json()
    assert "error" in data
    assert data["error"]["code"] == "NOT_FOUND" or data["error"]["code"] == "INTERNAL_ERROR"
