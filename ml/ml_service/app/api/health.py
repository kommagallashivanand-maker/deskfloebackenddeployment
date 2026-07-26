from fastapi import APIRouter
from ml.ml_service.app.core.config import settings

router = APIRouter()

@router.get("/health")
async def health():
    return {
        "status": "healthy"
    }

@router.get("/version")
async def version():
    return {
        "service": settings.SERVICE_NAME,
        "version": settings.SERVICE_VERSION,
    }