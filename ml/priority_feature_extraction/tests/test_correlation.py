from fastapi.testclient import TestClient
from app.main import app


def test_correlation_id_middleware_generates_id():
    client = TestClient(app)
    response = client.get("/health")
    assert response.status_code == 200
    assert "X-Correlation-ID" in response.headers
    correlation_id = response.headers["X-Correlation-ID"]
    assert len(correlation_id) > 0


def test_correlation_id_middleware_preserves_id():
    client = TestClient(app)
    test_id = "test-correlation-12345"
    response = client.get("/health", headers={"X-Correlation-ID": test_id})
    assert response.status_code == 200
    assert response.headers.get("X-Correlation-ID") == test_id
