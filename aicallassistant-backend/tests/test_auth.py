import time
import uuid
from unittest.mock import MagicMock
import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import ec

from app.api.deps import get_token_verifier
from app.core.auth import AuthenticatedUser, SupabaseTokenVerifier
from app.core.errors import UnauthorizedException
from app.main import app

TEST_HS256_SECRET = "super-secret-test-key-at-least-32-chars-long!"
TEST_SUPABASE_URL = "https://testproject.supabase.co"
TEST_USER_ID = str(uuid.uuid4())
TEST_EMAIL = "student@example.com"


@pytest.fixture
def ec_keypair():
    """Generates an ephemeral EC P-256 key pair for ES256 token tests."""
    private_key = ec.generate_private_key(ec.SECP256R1())
    public_key = private_key.public_key()
    return private_key, public_key


@pytest.fixture
def mock_jwks_client(ec_keypair):
    """Mocks jwt.PyJWKClient to return the test public key without making network requests."""
    _, public_key = ec_keypair
    client = MagicMock()
    signing_key = MagicMock()
    signing_key.key = public_key
    client.get_signing_key_from_jwt.return_value = signing_key
    return client


# -----------------------------------------------------------------------------
# 1. Unit Tests for SupabaseTokenVerifier
# -----------------------------------------------------------------------------


def test_verify_valid_hs256_token():
    verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwt_secret=TEST_HS256_SECRET,
    )
    now = int(time.time())
    payload = {
        "sub": TEST_USER_ID,
        "email": TEST_EMAIL,
        "role": "authenticated",
        "aud": "authenticated",
        "iss": f"{TEST_SUPABASE_URL}/auth/v1",
        "iat": now,
        "exp": now + 3600,
        "app_metadata": {"provider": "email"},
        "user_metadata": {"full_name": "Test Student"},
    }
    token = jwt.encode(payload, TEST_HS256_SECRET, algorithm="HS256")

    user = verifier.verify(token)
    assert isinstance(user, AuthenticatedUser)
    assert user.id == TEST_USER_ID
    assert user.email == TEST_EMAIL
    assert user.role == "authenticated"
    assert user.user_metadata["full_name"] == "Test Student"


def test_verify_valid_es256_jwks_token(ec_keypair, mock_jwks_client):
    private_key, _ = ec_keypair
    verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwks_client=mock_jwks_client,
    )
    now = int(time.time())
    payload = {
        "sub": TEST_USER_ID,
        "email": TEST_EMAIL,
        "role": "authenticated",
        "aud": "authenticated",
        "iss": f"{TEST_SUPABASE_URL}/auth/v1",
        "iat": now,
        "exp": now + 3600,
    }
    token = jwt.encode(payload, private_key, algorithm="ES256", headers={"kid": "test-key-id"})

    user = verifier.verify(token)
    assert user.id == TEST_USER_ID
    assert user.email == TEST_EMAIL
    mock_jwks_client.get_signing_key_from_jwt.assert_called_once_with(token)


def test_verify_expired_token():
    verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwt_secret=TEST_HS256_SECRET,
        leeway_seconds=0,
    )
    now = int(time.time())
    payload = {
        "sub": TEST_USER_ID,
        "aud": "authenticated",
        "iss": f"{TEST_SUPABASE_URL}/auth/v1",
        "iat": now - 7200,
        "exp": now - 3600,  # Expired 1 hour ago
    }
    token = jwt.encode(payload, TEST_HS256_SECRET, algorithm="HS256")

    with pytest.raises(UnauthorizedException) as exc_info:
        verifier.verify(token)
    assert "expired" in exc_info.value.message.lower()


def test_verify_invalid_signature():
    verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwt_secret=TEST_HS256_SECRET,
    )
    now = int(time.time())
    payload = {
        "sub": TEST_USER_ID,
        "aud": "authenticated",
        "iss": f"{TEST_SUPABASE_URL}/auth/v1",
        "iat": now,
        "exp": now + 3600,
    }
    wrong_secret = "different-secret-that-does-not-match-at-all!"
    token = jwt.encode(payload, wrong_secret, algorithm="HS256")

    with pytest.raises(UnauthorizedException) as exc_info:
        verifier.verify(token)
    assert "signature" in exc_info.value.message.lower()


def test_verify_invalid_audience():
    verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwt_secret=TEST_HS256_SECRET,
    )
    now = int(time.time())
    payload = {
        "sub": TEST_USER_ID,
        "aud": "wrong_audience",
        "iss": f"{TEST_SUPABASE_URL}/auth/v1",
        "iat": now,
        "exp": now + 3600,
    }
    token = jwt.encode(payload, TEST_HS256_SECRET, algorithm="HS256")

    with pytest.raises(UnauthorizedException) as exc_info:
        verifier.verify(token)
    assert "audience" in exc_info.value.message.lower()


def test_verify_invalid_issuer():
    verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwt_secret=TEST_HS256_SECRET,
    )
    now = int(time.time())
    payload = {
        "sub": TEST_USER_ID,
        "aud": "authenticated",
        "iss": "https://malicious-site.com/auth/v1",
        "iat": now,
        "exp": now + 3600,
    }
    token = jwt.encode(payload, TEST_HS256_SECRET, algorithm="HS256")

    with pytest.raises(UnauthorizedException) as exc_info:
        verifier.verify(token)
    assert "issuer" in exc_info.value.message.lower()


def test_verify_missing_sub_claim():
    verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwt_secret=TEST_HS256_SECRET,
    )
    now = int(time.time())
    payload = {
        "aud": "authenticated",
        "iss": f"{TEST_SUPABASE_URL}/auth/v1",
        "iat": now,
        "exp": now + 3600,
    }
    token = jwt.encode(payload, TEST_HS256_SECRET, algorithm="HS256")

    with pytest.raises(UnauthorizedException) as exc_info:
        verifier.verify(token)
    assert "sub" in exc_info.value.message.lower()


def test_verify_hs256_without_configured_secret():
    # Verifier initialized without secret
    verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwt_secret=None,
    )
    now = int(time.time())
    payload = {
        "sub": TEST_USER_ID,
        "aud": "authenticated",
        "iss": f"{TEST_SUPABASE_URL}/auth/v1",
        "exp": now + 3600,
    }
    token = jwt.encode(payload, "unconfigured-secret-at-least-32-chars-long!", algorithm="HS256")

    with pytest.raises(UnauthorizedException) as exc_info:
        verifier.verify(token)
    assert "SUPABASE_JWT_SECRET" in exc_info.value.message


def test_verify_empty_or_malformed_token():
    verifier = SupabaseTokenVerifier(jwt_secret=TEST_HS256_SECRET)
    with pytest.raises(UnauthorizedException):
        verifier.verify("")
    with pytest.raises(UnauthorizedException):
        verifier.verify("not.a.valid.jwt.structure")


# -----------------------------------------------------------------------------
# 2. Integration Tests with FastAPI HTTP Endpoint (/api/v1/auth/me)
# -----------------------------------------------------------------------------


def test_api_me_endpoint_missing_authorization(client):
    response = client.get("/api/v1/auth/me")
    assert response.status_code == 401
    data = response.json()
    assert "error" in data
    assert data["error"]["code"] == "UNAUTHORIZED"
    assert "missing" in data["error"]["message"].lower()


def test_api_me_endpoint_success(client):
    test_verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwt_secret=TEST_HS256_SECRET,
    )
    app.dependency_overrides[get_token_verifier] = lambda: test_verifier

    try:
        now = int(time.time())
        payload = {
            "sub": TEST_USER_ID,
            "email": TEST_EMAIL,
            "role": "authenticated",
            "aud": "authenticated",
            "iss": f"{TEST_SUPABASE_URL}/auth/v1",
            "iat": now,
            "exp": now + 3600,
        }
        token = jwt.encode(payload, TEST_HS256_SECRET, algorithm="HS256")

        response = client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert response.status_code == 200
        data = response.json()
        assert data["user_id"] == TEST_USER_ID
        assert data["email"] == TEST_EMAIL
        assert data["role"] == "authenticated"
    finally:
        app.dependency_overrides.clear()


def test_api_me_endpoint_expired_token(client):
    test_verifier = SupabaseTokenVerifier(
        supabase_url=TEST_SUPABASE_URL,
        jwt_secret=TEST_HS256_SECRET,
        leeway_seconds=0,
    )
    app.dependency_overrides[get_token_verifier] = lambda: test_verifier

    try:
        now = int(time.time())
        payload = {
            "sub": TEST_USER_ID,
            "email": TEST_EMAIL,
            "aud": "authenticated",
            "iss": f"{TEST_SUPABASE_URL}/auth/v1",
            "iat": now - 7200,
            "exp": now - 3600,
        }
        token = jwt.encode(payload, TEST_HS256_SECRET, algorithm="HS256")

        response = client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert response.status_code == 401
        data = response.json()
        assert data["error"]["code"] == "UNAUTHORIZED"
        assert "expired" in data["error"]["message"].lower()
    finally:
        app.dependency_overrides.clear()
