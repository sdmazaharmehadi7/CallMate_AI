import logging
from app.core.config import Settings
from app.core.logging import SensitiveDataFilter


def test_settings_defaults():
    settings = Settings()
    assert settings.PORT == 8000
    assert settings.ENVIRONMENT == "development"
    assert "http://10.0.2.2:8000" in settings.ALLOWED_ORIGINS
    assert "http://10.165.44.67:8000" in settings.ALLOWED_ORIGINS


def test_cors_origins_string_parsing():
    settings = Settings(ALLOWED_ORIGINS="http://example.com, https://app.example.com")
    assert isinstance(settings.ALLOWED_ORIGINS, list)
    assert len(settings.ALLOWED_ORIGINS) == 2
    assert "http://example.com" in settings.ALLOWED_ORIGINS


def test_sensitive_data_filter_masks_tokens():
    log_filter = SensitiveDataFilter()
    record = logging.LogRecord(
        name="test",
        level=logging.INFO,
        pathname="",
        lineno=0,
        msg="User authenticated with Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.secretpayload.sig",
        args=(),
        exc_info=None,
    )
    log_filter.filter(record)
    assert "Bearer [REDACTED_JWT]" in record.msg
    assert "secretpayload" not in record.msg


def test_sensitive_data_filter_masks_audio_payload():
    log_filter = SensitiveDataFilter()
    record = logging.LogRecord(
        name="test",
        level=logging.DEBUG,
        pathname="",
        lineno=0,
        msg="Frame received: {'event': 'audio_in', 'data': 'UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA='}",
        args=(),
        exc_info=None,
    )
    log_filter.filter(record)
    assert "[AUDIO_DATA_OMITTED]" in record.msg
