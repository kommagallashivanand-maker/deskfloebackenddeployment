import json
from app.core.config import settings
from typing import Optional


def _load_mapping() -> dict:
    try:
        with open(settings.category_mapping_path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception:
        return {}


_MAPPING = _load_mapping()


def normalize_category(category: Optional[str]) -> str:
    # Preserve the original category by default. Mapping is applied only when enabled in settings.
    if not category:
        return "Unknown"
    key = category.strip()
    if not settings.use_category_mapping:
        return key
    return _MAPPING.get(key, key)
