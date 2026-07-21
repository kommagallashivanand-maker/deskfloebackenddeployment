from fastapi.testclient import TestClient
from app.main import app
from app.models.ticket import TicketIn, AuthContext
from app.feature_extraction.feature_extractor import extract_features

client = TestClient(app)


def test_extract_roundtrip():
    payload = {
        "ticket": {"subject": "VPN not connecting", "description": "Cannot connect to VPN", "category": "Network"},
        "auth": {"user_id": "u1", "role": "employee"}
    }
    r = client.post("/extract", json=payload)
    assert r.status_code == 200
    data = r.json()
    assert "keywords" in data
    assert "sentiment" in data
    assert "description_length" in data
    assert data["description_length"] == len("Cannot connect to VPN")


def test_description_length_cases():
    auth = AuthContext(user_id="u1", role="employee")

    # 1. Normal description
    desc = "Cannot connect to VPN"
    ticket = TicketIn(subject="VPN", description=desc, category="Network")
    features = extract_features(ticket, auth)
    assert features.description_length == len(desc)

    # 2. Empty description
    ticket_empty = TicketIn(subject="VPN", description="", category="Network")
    features_empty = extract_features(ticket_empty, auth)
    assert features_empty.description_length == 0

    # 3. None/Null description
    ticket_none = TicketIn(subject="VPN", description=None, category="Network")
    features_none = extract_features(ticket_none, auth)
    assert features_none.description_length == 0

    # 4. Very long description
    long_desc = "X" * 15000
    ticket_long = TicketIn(subject="VPN", description=long_desc, category="Network")
    features_long = extract_features(ticket_long, auth)
    assert features_long.description_length == 15000

