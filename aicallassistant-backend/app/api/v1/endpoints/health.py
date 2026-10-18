from datetime import datetime, timezone
from fastapi import APIRouter
from app.core.config import settings
from app.models.schemas import HealthResponse

router = APIRouter(tags=["Health"])


@router.get("/health", response_model=HealthResponse)
async def health_check() -> HealthResponse:
    """Service health check returning server status, version, environment, and Gemini config state."""
    return HealthResponse(
        status="healthy",
        version="0.1.0",
        environment=settings.ENVIRONMENT,
        gemini_configured=settings.is_gemini_configured(),
        timestamp=datetime.now(timezone.utc),
    )

