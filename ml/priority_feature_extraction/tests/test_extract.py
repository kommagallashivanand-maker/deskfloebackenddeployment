from fastapi.testclient import TestClient
import json
import os

from app.main import app

client = TestClient(app)


def load_sample():
    path = os.path.join(os.path.dirname(__file__), "..", "sample_data", "sample_tickets.json")
    path = os.path.abspath(path)
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def test_extract_endpoint():
    ticket = {
        "subject": "VPN not connecting",
        "description": "I cannot connect to VPN since this morning, urgent for client meeting",
        "category": "Network"
    }
    auth = {"user_id": "u1", "role": "employee"}
    payload = {"ticket": ticket, "auth": auth}
    resp = client.post("/extract", json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert "keywords" in data
    assert "sentiment" in data
    assert "category" in data
    assert "requester_role" in data
