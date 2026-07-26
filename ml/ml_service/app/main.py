from fastapi import FastAPI

from ml.ml_service.app.api.health import router as health_router
from ml.ml_service.app.core.config import settings
from ml.ml_service.app.core.logging import setup_logging
from ml.ml_service.app.middleware.correlation import CorrelationIdMiddleware

from ml.similar_tickets.api.routes import (
    router as similar_tickets_router,
)
from ml.similar_tickets.api.embedding_routes import (
    router as embedding_router,
)

setup_logging()

app = FastAPI(
    title=settings.SERVICE_NAME,
    version=settings.SERVICE_VERSION,
)

app.add_middleware(CorrelationIdMiddleware)

app.include_router(health_router)
app.include_router(similar_tickets_router)
app.include_router(embedding_router)