from app.feature_extraction.keyword_extractor import extract_keywords


def test_keywords_returns_list():
    kws = extract_keywords("VPN down", "VPN error cannot connect", top=3)
    assert isinstance(kws, list)
