import logging

from fastapi import APIRouter, HTTPException

from app.core.config import settings
from registry.registry import RegistryError

router = APIRouter()
logger = logging.getLogger(__name__)


@router.get("/health")
async def health():
    return {"status": "healthy"}


@router.get("/version")
async def version():
    """
    Return service version and the active model version for each family.

    Response
    --------
    {
        "service": "DeskFlow ML Service",
        "version": "1.0.0",
        "models": {
            "category": "embedding_v1",
            "priority": "priority_v1"
        }
    }
    """
    try:
        from registry.loader import get_registry

        registry = get_registry()
        active_models = registry.active_versions()
    except RegistryError as exc:
        logger.error("Registry error on /version: %s", exc)
        raise HTTPException(status_code=500, detail=str(exc))

    return {
        "service": settings.SERVICE_NAME,
        "version": settings.SERVICE_VERSION,
        "models": active_models,
    }
