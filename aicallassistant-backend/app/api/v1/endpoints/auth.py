from fastapi import APIRouter, Depends
from app.api.deps import get_current_user
from app.core.auth import AuthenticatedUser

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.get("/me", summary="Get currently authenticated user identity")
async def get_me(current_user: AuthenticatedUser = Depends(get_current_user)):
    """Returns the authenticated user details extracted from the verified Supabase JWT."""
    return {
        "user_id": current_user.id,
        "email": current_user.email,
        "role": current_user.role,
    }
