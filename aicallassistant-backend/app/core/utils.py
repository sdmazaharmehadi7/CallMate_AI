import uuid
from typing import Set
from app.core.errors import BadRequestException

# Allowed status transitions based on voice call lifecycle
VALID_CALL_TRANSITIONS = {
    "connecting": {"active", "completed", "failed", "cancelled"},
    "active": {"completed", "failed", "cancelled"},
    "completed": set(),
    "failed": set(),
    "cancelled": set(),
}


def validate_uuid(value: str, param_name: str = "id") -> str:
    """
    Validates that a string is formatted as a valid UUID.
    Returns the normalized canonical string format.
    Raises BadRequestException if invalid.
    """
    if not value or not isinstance(value, str):
        raise BadRequestException(f"Invalid {param_name}: value must be a non-empty string UUID")
    try:
        parsed = uuid.UUID(value.strip())
        return str(parsed)
    except (ValueError, AttributeError, TypeError):
        raise BadRequestException(f"Invalid {param_name}: '{value}' is not a valid UUID")


def validate_status_transition(current_status: str, new_status: str) -> None:
    """
    Validates that transitioning from current_status to new_status is allowed.
    Raises BadRequestException if the transition is prohibited.
    """
    allowed_next: Set[str] = VALID_CALL_TRANSITIONS.get(current_status, set())
    if new_status == current_status:
        return
    if new_status not in allowed_next:
        raise BadRequestException(
            f"Invalid status transition: cannot transition call from '{current_status}' to '{new_status}'"
        )
