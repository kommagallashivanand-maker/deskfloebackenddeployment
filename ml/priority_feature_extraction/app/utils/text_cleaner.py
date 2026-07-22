from app.feature_extraction.preprocessing import normalize_text


def clean(text: str) -> str:
    return normalize_text(text)
