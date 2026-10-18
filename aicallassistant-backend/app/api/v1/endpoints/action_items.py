from fastapi import APIRouter, Depends

from app.api.deps import get_current_user_id, get_db_service
from app.core.errors import NotFoundException
from app.core.utils import validate_uuid
from app.models.schemas import ActionItem, DeleteResponse, UpdateActionItemRequest
from app.services.database import DatabaseService

router = APIRouter(prefix="/action-items", tags=["Action Items"])


@router.patch(
    "/{action_item_id}",
    response_model=ActionItem,
    summary="Update action item status or details",
)
async def update_action_item(
    action_item_id: str,
    request: UpdateActionItemRequest,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> ActionItem:
    """
    Updates an action item's completion status, task description, or due date.
    Only allows modifications to items owned through their parent call.
    """
    valid_id = validate_uuid(action_item_id, "action_item_id")
    updates = request.model_dump(exclude_unset=True)

    updated = db.update_action_item(valid_id, user_id, updates)
    if not updated:
        raise NotFoundException("Action item not found")

    return ActionItem(**updated)


@router.delete(
    "/{action_item_id}",
    response_model=DeleteResponse,
    summary="Delete action item",
)
async def delete_action_item(
    action_item_id: str,
    user_id: str = Depends(get_current_user_id),
    db: DatabaseService = Depends(get_db_service),
) -> DeleteResponse:
    """
    Deletes an action item. Only allows deletion if the caller owns the parent call.
    """
    valid_id = validate_uuid(action_item_id, "action_item_id")
    deleted = db.delete_action_item(valid_id, user_id)
    if not deleted:
        raise NotFoundException("Action item not found")

    return DeleteResponse(deleted=True, id=valid_id)
