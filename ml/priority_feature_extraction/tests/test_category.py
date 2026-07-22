from app.feature_extraction.category_feature import normalize_category
from app.core.config import settings


def test_category_mapping_disabled():
    # Verify default behavior when mapping is disabled (returns the stripped raw category)
    assert settings.use_category_mapping is False
    assert normalize_category("Network") == "Network"
    assert normalize_category("  Billing  ") == "Billing"


def test_category_mapping_enabled(monkeypatch):
    # Verify behavior when mapping is enabled (translates configured keys)
    monkeypatch.setattr(settings, "use_category_mapping", True)
    assert normalize_category("Network") == "Technical"
    assert normalize_category("Billing") == "Billing"


def test_category_mapping_fallback(monkeypatch):
    # Verify fallback to original category if not configured in the mapping json
    monkeypatch.setattr(settings, "use_category_mapping", True)
    assert normalize_category("Hardware") == "Hardware"


def test_category_mapping_empty():
    # Verify empty/null categories return "Unknown"
    assert normalize_category("") == "Unknown"
    assert normalize_category(None) == "Unknown"

