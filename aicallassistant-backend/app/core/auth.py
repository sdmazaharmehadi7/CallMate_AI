from typing import Any, Dict, List, Optional
import jwt
from pydantic import BaseModel, Field

from app.core.config import settings
from app.core.errors import UnauthorizedException
from app.core.logging import logger


class AuthenticatedUser(BaseModel):
    """Represents a validated Supabase user identity extracted from a verified JWT."""

    id: str = Field(..., description="Unique user identifier (sub claim)")
    email: Optional[str] = Field(default=None, description="User email address")
    role: str = Field(default="authenticated", description="User role in Supabase Auth")
    app_metadata: Dict[str, Any] = Field(default_factory=dict)
    user_metadata: Dict[str, Any] = Field(default_factory=dict)


class SupabaseTokenVerifier:
    """
    Validates Supabase Auth access tokens.
    
    Supports:
    1. Modern Asymmetric JWKS verification (ES256, RS256) via Supabase Auth .well-known endpoint.
    2. Optional Symmetric verification (HS256) when SUPABASE_JWT_SECRET is explicitly configured.
    """

    SUPPORTED_ASYMMETRIC_ALGORITHMS = ["ES256", "RS256"]
    SUPPORTED_SYMMETRIC_ALGORITHMS = ["HS256"]

    def __init__(
        self,
        supabase_url: Optional[str] = None,
        jwt_secret: Optional[str] = None,
        jwks_client: Optional[jwt.PyJWKClient] = None,
        expected_audience: str = "authenticated",
        leeway_seconds: int = 10,
    ):
        self.supabase_url = (supabase_url or settings.SUPABASE_URL).rstrip("/")
        self.jwt_secret = jwt_secret if jwt_secret is not None else settings.SUPABASE_JWT_SECRET
        self.expected_audience = expected_audience
        self.leeway_seconds = leeway_seconds

        # Configure or inject JWKS client for asymmetric algorithms (ES256/RS256)
        if jwks_client is not None:
            self._jwks_client = jwks_client
        elif self.supabase_url and not self.supabase_url.startswith("https://placeholder"):
            jwks_url = f"{self.supabase_url}/auth/v1/.well-known/jwks.json"
            self._jwks_client = jwt.PyJWKClient(jwks_url, cache_jwk_set=True, lifespan=3600)
        else:
            self._jwks_client = None

    def _get_expected_issuer(self) -> Optional[str]:
        if self.supabase_url and not self.supabase_url.startswith("https://placeholder"):
            return f"{self.supabase_url}/auth/v1"
        return None

    def verify(self, token: str) -> AuthenticatedUser:
        """
        Validates token signature, expiration, issuer, audience, and claims.
        Returns an AuthenticatedUser instance if verification succeeds.
        """
        if not token or not token.strip():
            raise UnauthorizedException("Token is empty or missing")

        # 1. Inspect unverified header to determine signing algorithm and key ID
        try:
            header = jwt.get_unverified_header(token)
        except Exception as e:
            logger.warning("Failed to decode token header: %s", str(e))
            raise UnauthorizedException("Malformed token header")

        alg = header.get("alg")
        if not alg:
            raise UnauthorizedException("Token missing algorithm declaration ('alg')")

        # 2. Select verification key based on algorithm
        signing_key: Any = None
        if alg in self.SUPPORTED_ASYMMETRIC_ALGORITHMS:
            if self._jwks_client is None:
                raise UnauthorizedException(
                    f"Token signed with {alg}, but JWKS client is not configured. Verify SUPABASE_URL."
                )
            try:
                signing_key = self._jwks_client.get_signing_key_from_jwt(token).key
            except Exception as e:
                logger.warning("JWKS key retrieval failed for kid=%s: %s", header.get("kid"), str(e))
                raise UnauthorizedException("Could not find matching public key in Supabase JWKS")

        elif alg in self.SUPPORTED_SYMMETRIC_ALGORITHMS:
            if not self.jwt_secret:
                raise UnauthorizedException(
                    "Token is signed with HS256, but SUPABASE_JWT_SECRET is not configured on the backend"
                )
            signing_key = self.jwt_secret

        else:
            raise UnauthorizedException(
                f"Unsupported token algorithm '{alg}'. Supported: {self.SUPPORTED_ASYMMETRIC_ALGORITHMS + self.SUPPORTED_SYMMETRIC_ALGORITHMS}"
            )

        # 3. Decode and verify claims
        decode_kwargs: Dict[str, Any] = {
            "algorithms": [alg],
            "audience": self.expected_audience,
            "leeway": self.leeway_seconds,
            "options": {
                "require": ["exp", "sub", "aud"],
                "verify_exp": True,
                "verify_aud": True,
            },
        }

        expected_issuer = self._get_expected_issuer()
        if expected_issuer:
            decode_kwargs["issuer"] = expected_issuer
            decode_kwargs["options"]["verify_iss"] = True

        try:
            payload = jwt.decode(token, signing_key, **decode_kwargs)
        except jwt.ExpiredSignatureError:
            raise UnauthorizedException("Token has expired")
        except jwt.InvalidAudienceError:
            raise UnauthorizedException(f"Invalid token audience (expected: {self.expected_audience})")
        except jwt.InvalidIssuerError:
            raise UnauthorizedException(f"Invalid token issuer (expected: {expected_issuer})")
        except jwt.InvalidSignatureError:
            raise UnauthorizedException("Invalid token signature")
        except jwt.DecodeError as e:
            raise UnauthorizedException(f"Token decode error: {str(e)}")
        except jwt.PyJWTError as e:
            raise UnauthorizedException(f"Token verification failed: {str(e)}")

        # 4. Extract subject UUID
        user_id = payload.get("sub")
        if not user_id or not isinstance(user_id, str):
            raise UnauthorizedException("Token missing valid user identifier ('sub')")

        return AuthenticatedUser(
            id=user_id,
            email=payload.get("email"),
            role=payload.get("role", "authenticated"),
            app_metadata=payload.get("app_metadata", {}),
            user_metadata=payload.get("user_metadata", {}),
        )


# Singleton token verifier instance
default_token_verifier = SupabaseTokenVerifier()
