from typing import Optional
from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.core.auth import AuthenticatedUser, SupabaseTokenVerifier, default_token_verifier
from app.core.errors import UnauthorizedException

# HTTP Bearer scheme without auto_error to return custom JSON error responses
oauth2_scheme = HTTPBearer(auto_error=False)


def get_token_verifier() -> SupabaseTokenVerifier:
    """Dependency providing the configured SupabaseTokenVerifier instance."""
    return default_token_verifier


async def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(oauth2_scheme),
    verifier: SupabaseTokenVerifier = Depends(get_token_verifier),
) -> AuthenticatedUser:
    """
    Validates the Bearer token in the Authorization header.
    Returns the authenticated user details or raises UnauthorizedException.
    """
    if credentials is None or not credentials.credentials:
        raise UnauthorizedException("Missing or malformed Authorization header. Expected Bearer token.")

    return verifier.verify(credentials.credentials)


async def get_current_user_id(
    current_user: AuthenticatedUser = Depends(get_current_user),
) -> str:
    """
    Convenience dependency returning strictly the authenticated user's unique UUID.
    """
    return current_user.id


def get_db_service():
    """Dependency providing the active DatabaseService instance."""
    from app.services.database import get_database_service
    return get_database_service()

