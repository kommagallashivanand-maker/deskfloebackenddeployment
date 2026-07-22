import logging
import time
import uuid
from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware

logger = logging.getLogger(__name__)

class CorrelationIdMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        correlation_id = request.headers.get(
            "X-Correlation-ID",
            str(uuid.uuid4())
        )

        start = time.perf_counter()

        logger.info(
            "[%s] Incoming %s %s",
            correlation_id,
            request.method,
            request.url.path,
        )

        response = await call_next(request)
        duration = (time.perf_counter() - start) * 1000

        logger.info(
            "[%s] Completed %s %s -> %d (%.2f ms)",
            correlation_id,
            request.method,
            request.url.path,
            response.status_code,
            duration,
        )

        response.headers["X-Correlation-ID"] = correlation_id

        return response