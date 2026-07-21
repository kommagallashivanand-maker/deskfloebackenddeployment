from app.feature_extraction.requester_role import normalize_role


def test_normalize_role():
    assert normalize_role("mgr") == "manager"
    assert normalize_role(None) == "unknown"
