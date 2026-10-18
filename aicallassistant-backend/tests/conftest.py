import time
import uuid
import jwt
import pytest
from fastapi.testclient import TestClient

from app.api.deps import get_db_service, get_token_verifier
from app.core.auth import SupabaseTokenVerifier
from app.main import app
from app.services.database import InMemoryDatabaseService

TEST_HS256_SECRET = "super-secret-test-key-at-least-32-chars-long!"
TEST_SUPABASE_URL = "https://testproject.supabase.co"

USER_A_ID = str(uuid.uuid4())
USER_B_ID = str(uuid.uuid4())


def create_test_token(user_id: str, email: str = "test@example.com", expires_in: int = 3600) -> str:
    now = int(time.time())
    payload = {
        "sub": user_id,
        "email": email,
        "role": "authenticated",
        "aud": "authenticated",
        "iss": f"{TEST_SUPABASE_URL}/auth/v1",
        "iat": now,
        "exp": now + expires_in,
        "app_metadata": {"provider": "email"},
        "user_metadata": {"full_name": f"User {user_id[:6]}"},
    }
    return jwt.encode(payload, TEST_HS256_SECRET, algorithm="HS256")


@pytest.fixture
def test_db():
    """Provides a fresh isolated InMemoryDatabaseService per test."""
    return InMemoryDatabaseService()


@pytest.fixture
def client(test_db):
    """
    Provides a synchronous FastAPI TestClient wired with test token verifier
    and isolated in-memory database service. Cleans up overrides after each test.
    """
    verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwt_secret=TEST_HS256_SECRET,
    )
    app.dependency_overrides[get_token_verifier] = lambda: verifier
    app.dependency_overrides[get_db_service] = lambda: test_db

    with TestClient(app) as test_client:
        yield test_client

    app.dependency_overrides.clear()


@pytest.fixture
def user_a_headers():
    token = create_test_token(USER_A_ID, "user_a@example.com")
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def user_b_headers():
    token = create_test_token(USER_B_ID, "user_b@example.com")
    return {"Authorization": f"Bearer {token}"}
