from fastapi import APIRouter
from app.core.config import settings

router = APIRouter()


@router.get("/health")
def health_check():
    try:
        app_name = settings.app_name
    except AttributeError:
        app_name = "unknown"
    return {"status": "ok", "app": app_name}
