from fastapi import FastAPI, HTTPException
from app.core.config import settings
from app.models.ticket import TicketIn, AuthContext
from app.models.features import FeatureOut
from app.feature_extraction.feature_extractor import extract_features
from app.core.logging import get_logger
from app.api.health import router as health_router
from app.middleware.correlation import CorrelationIdMiddleware

logger = get_logger("app.main")
app = FastAPI(title=settings.app_name)

app.add_middleware(CorrelationIdMiddleware)
app.include_router(health_router)


@app.post("/extract", response_model=FeatureOut)
def extract(payload: dict):
    try:
        ticket = TicketIn(**payload.get("ticket", {}))
        auth = AuthContext(**payload.get("auth", {}))
        features = extract_features(ticket, auth)
        return features
    except Exception as e:
        logger.exception("feature extraction failed")
        raise HTTPException(status_code=500, detail=str(e))

