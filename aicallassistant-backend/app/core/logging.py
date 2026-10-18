import logging
import re
import sys
from typing import Any, Dict


class SensitiveDataFilter(logging.Filter):
    """
    Sanitizes log messages by masking API keys, JWT tokens, and stripping
    raw audio payloads and sensitive call content.
    """

    # Regex patterns for sensitive keys and tokens
    TOKEN_PATTERN = re.compile(r"(Bearer\s+)[A-Za-z0-9\-_\.]+", re.IGNORECASE)
    KEY_PATTERN = re.compile(r"(api[_-]?key|secret|token)['\"]?\s*[:=]\s*['\"]?([A-Za-z0-9\-_]+)", re.IGNORECASE)
    BASE64_AUDIO_PATTERN = re.compile(r"('data':\s*')[A-Za-z0-9+/=]{50,}(')", re.IGNORECASE)

    def filter(self, record: logging.LogRecord) -> bool:
        if isinstance(record.msg, str):
            msg = record.msg
            msg = self.TOKEN_PATTERN.sub(r"\1[REDACTED_JWT]", msg)
            msg = self.KEY_PATTERN.sub(r"\1=[REDACTED]", msg)
            msg = self.BASE64_AUDIO_PATTERN.sub(r"\1[AUDIO_DATA_OMITTED]\2", msg)
            record.msg = msg
        return True


def setup_logging(log_level: str = "INFO") -> None:
    """Configures application-wide logging with sensitive data filtering."""
    handler = logging.StreamHandler(sys.stdout)
    handler.addFilter(SensitiveDataFilter())
    handler.setFormatter(
        logging.Formatter(
            fmt="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
        )
    )

    root_logger = logging.getLogger()
    root_logger.setLevel(getattr(logging, log_level.upper(), logging.INFO))
    root_logger.handlers = [handler]

    # Silence overly verbose third-party loggers
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)


logger = logging.getLogger("aicallassistant")
