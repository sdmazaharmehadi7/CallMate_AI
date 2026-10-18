from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.endpoints import action_items, auth, calls, health, stream
from app.core.config import settings
from app.core.errors import register_error_handlers
from app.core.logging import logger, setup_logging


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan manager for startup and shutdown events."""
    setup_logging(log_level=settings.LOG_LEVEL)
    logger.info(
        "AI Call Assistant backend started successfully. Environment: %s | Host: %s:%d",
        settings.ENVIRONMENT,
        settings.HOST,
        settings.PORT,
    )
    yield
    logger.info("AI Call Assistant backend shutting down.")


def create_application() -> FastAPI:
    """Factory function for building the configured FastAPI application."""
    app = FastAPI(
        title="AI Call Assistant Backend",
        description="Backend service orchestrating simulated AI call answering, voice streaming, and call records.",
        version="0.1.0",
        docs_url="/docs" if not settings.is_production() else None,
        redoc_url="/redoc" if not settings.is_production() else None,
        lifespan=lifespan,
    )

    # Configure CORS for local Android development and Web clients
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.ALLOWED_ORIGINS,
        allow_credentials=True,
        allow_methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"],
        allow_headers=["*"],
    )

    # Register centralized error handlers
    register_error_handlers(app)

    # Include API routers
    app.include_router(health.router)
    app.include_router(health.router, prefix="/api/v1")
    app.include_router(auth.router, prefix="/api/v1")
    app.include_router(calls.router, prefix="/api/v1")
    app.include_router(action_items.router, prefix="/api/v1")
    app.include_router(stream.router)
    app.include_router(stream.router, prefix="/api/v1")

    return app


app = create_application()
