from app.feature_extraction.preprocessing import normalize_text


def test_normalize_basic():
    s = "  Hello, WORLD!!!\nNew line"
    out = normalize_text(s)
    assert "hello" in out
    # exclamation marks and commas should be stripped
    assert "!" not in out
    assert "," not in out
    # newlines should be replaced with spaces
    assert "\n" not in out
    # output should be lowercase
    assert out == out.lower()
