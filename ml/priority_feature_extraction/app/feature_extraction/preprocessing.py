import re
from typing import Optional


def normalize_text(text: Optional[str]) -> str:
    if not text:
        return ""
    s = text.lower()
    s = re.sub(r"[\r\n]+", " ", s)
    s = re.sub(r"[^\w\s'-]", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s
