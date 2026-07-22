from typing import Optional

ROLE_MAP = {
    "mgr": "manager",
    "admin": "admin",
    "employee": "employee",
    "intern": "intern",
}


def normalize_role(role: Optional[str]) -> str:
    if not role:
        return "unknown"
    r = role.strip().lower()
    return ROLE_MAP.get(r, r)
