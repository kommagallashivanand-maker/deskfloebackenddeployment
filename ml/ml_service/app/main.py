from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.health import router as health_router
from app.core.config import settings
from app.core.logging import setup_logging
from app.middleware.correlation import CorrelationIdMiddleware

setup_logging()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Pre-load all active models into memory before the first request."""
    from registry.loader import preload_active_models

    preload_active_models()
    yield


app = FastAPI(
    title=settings.SERVICE_NAME,
    version=settings.SERVICE_VERSION,
    lifespan=lifespan,
)

app.add_middleware(CorrelationIdMiddleware)

app.include_router(health_router)
