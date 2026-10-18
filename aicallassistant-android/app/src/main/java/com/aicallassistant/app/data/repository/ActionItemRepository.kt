package com.aicallassistant.app.data.repository

import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.remote.ApiClient
import com.aicallassistant.app.data.remote.UpdateActionItemDto
import com.aicallassistant.app.domain.model.ActionItem

interface ActionItemRepository {
    suspend fun getActionItems(callId: String): NetworkResult<List<ActionItem>>
    suspend fun createActionItem(callId: String, task: String, dueDate: String? = null): NetworkResult<ActionItem>
    suspend fun toggleCompleted(actionItemId: String, completed: Boolean): NetworkResult<ActionItem>
    suspend fun updateTask(actionItemId: String, task: String): NetworkResult<ActionItem>
    suspend fun deleteActionItem(actionItemId: String): NetworkResult<Boolean>
}

class DefaultActionItemRepository(
    private val apiClient: ApiClient = ApiClient()
) : ActionItemRepository {

    override suspend fun getActionItems(callId: String): NetworkResult<List<ActionItem>> {
        return when (val res = apiClient.getActionItems(callId)) {
            is NetworkResult.Success -> {
                val list = res.data.actionItems.map { a ->
                    ActionItem(
                        id = a.id,
                        callId = a.callId,
                        task = a.task,
                        dueDate = a.dueDate,
                        completed = a.completed,
                        completedAt = a.completedAt,
                        createdAt = a.createdAt
                    )
                }
                NetworkResult.Success(list)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun createActionItem(callId: String, task: String, dueDate: String?): NetworkResult<ActionItem> {
        return when (val res = apiClient.createActionItem(callId, task, dueDate)) {
            is NetworkResult.Success -> {
                val a = res.data
                NetworkResult.Success(
                    ActionItem(
                        id = a.id,
                        callId = a.callId,
                        task = a.task,
                        dueDate = a.dueDate,
                        completed = a.completed,
                        completedAt = a.completedAt,
                        createdAt = a.createdAt
                    )
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun toggleCompleted(actionItemId: String, completed: Boolean): NetworkResult<ActionItem> {
        return when (val res = apiClient.patchActionItem(actionItemId, UpdateActionItemDto(completed = completed))) {
            is NetworkResult.Success -> {
                val a = res.data
                NetworkResult.Success(
                    ActionItem(
                        id = a.id,
                        callId = a.callId,
                        task = a.task,
                        dueDate = a.dueDate,
                        completed = a.completed,
                        completedAt = a.completedAt,
                        createdAt = a.createdAt
                    )
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun updateTask(actionItemId: String, task: String): NetworkResult<ActionItem> {
        return when (val res = apiClient.patchActionItem(actionItemId, UpdateActionItemDto(task = task))) {
            is NetworkResult.Success -> {
                val a = res.data
                NetworkResult.Success(
                    ActionItem(
                        id = a.id,
                        callId = a.callId,
                        task = a.task,
                        dueDate = a.dueDate,
                        completed = a.completed,
                        completedAt = a.completedAt,
                        createdAt = a.createdAt
                    )
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun deleteActionItem(actionItemId: String): NetworkResult<Boolean> {
        return when (val res = apiClient.deleteActionItem(actionItemId)) {
            is NetworkResult.Success -> NetworkResult.Success(res.data.deleted)
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }
}
