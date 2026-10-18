from typing import List, Optional, Union
from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application configuration loaded from environment variables."""

    # Server Settings
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    ENVIRONMENT: str = "development"
    LOG_LEVEL: str = "INFO"

    # CORS Settings
    ALLOWED_ORIGINS: Union[str, List[str]] = [
        "http://localhost:3000",
        "http://127.0.0.1:8000",
        "http://10.0.2.2:8000",
        "http://10.165.44.67:8000",
    ]

    @field_validator("ALLOWED_ORIGINS", mode="before")
    @classmethod
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        if isinstance(v, str):
            return [i.strip() for i in v.split(",") if i.strip()]
        return v

    # Gemini Live API Settings
    GEMINI_API_KEY: str = Field(default="test_placeholder_key")
    GEMINI_MODEL: str = "gemini-2.5-flash-native-audio-latest"
    GEMINI_DEFAULT_VOICE: str = "Aoede"
    GEMINI_LIVE_CONNECTION_TIMEOUT_SECONDS: int = 15
    GEMINI_SESSION_MAX_DURATION_SECONDS: int = 900  # Max 15 minutes per session
    GEMINI_MAX_AUDIO_CHUNK_BYTES: int = 65536  # 64 KB limit per audio chunk frame

    # Supabase Settings
    SUPABASE_URL: str = Field(default="https://placeholder.supabase.co")
    SUPABASE_ANON_KEY: str = Field(default="placeholder_anon_key")
    SUPABASE_PUBLISHABLE_KEY: Optional[str] = None
    SUPABASE_SERVICE_ROLE_KEY: str = Field(default="placeholder_service_role_key")
    SUPABASE_JWT_SECRET: Optional[str] = None
    SUPABASE_STORAGE_BUCKET: str = "call-recordings"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )

    def is_production(self) -> bool:
        return self.ENVIRONMENT.lower() == "production"

    def is_gemini_configured(self) -> bool:
        """Checks whether a non-placeholder Gemini API key is configured."""
        if not self.GEMINI_API_KEY:
            return False
        key = self.GEMINI_API_KEY.strip().lower()
        return not any(
            placeholder in key
            for placeholder in ["placeholder", "your_gemini", "test_placeholder"]
        )


settings = Settings()
