package com.aicallassistant.app

import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.repository.ActionItemRepository
import com.aicallassistant.app.data.repository.CallRepository
import com.aicallassistant.app.domain.model.ActionItem
import com.aicallassistant.app.domain.model.CallItem
import com.aicallassistant.app.domain.model.CallStatus
import com.aicallassistant.app.domain.model.CallType
import com.aicallassistant.app.domain.model.UrgencyLevel
import com.aicallassistant.app.ui.viewmodel.ActionItemsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActionItemsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val callRepository = mockk<CallRepository>(relaxed = true)
    private val actionItemRepository = mockk<ActionItemRepository>(relaxed = true)

    private val testCall = CallItem(
        id = "call-1",
        callerName = "Doctor",
        callerNumber = "+919876543210",
        status = CallStatus.COMPLETED,
        callType = CallType.SIMULATED,
        callerReason = "Appointment",
        summary = "Appointment confirmed",
        urgency = UrgencyLevel.HIGH,
        isImportant = true,
        durationSeconds = 60,
        hasRecording = false,
        createdAt = "2026-10-18T10:00:00Z"
    )

    private val testItem = ActionItem(
        id = "action-1",
        callId = "call-1",
        task = "Pick up prescription",
        dueDate = null,
        completed = false,
        completedAt = null,
        createdAt = "2026-10-18T10:01:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { callRepository.getCalls(any(), any(), any(), any(), any()) } returns NetworkResult.Success(Pair(listOf(testCall), 1))
        coEvery { actionItemRepository.getActionItems("call-1") } returns NetworkResult.Success(listOf(testItem))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadActionItems_aggregatesItemsFromCalls() = runTest {
        val viewModel = ActionItemsViewModel(callRepository, actionItemRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.actionItems.size)
        assertEquals("action-1", state.actionItems[0].id)
        assertEquals("Pick up prescription", state.actionItems[0].task)
    }

    @Test
    fun toggleCompleted_callsRepositoryAndUpdatesState() = runTest {
        val updatedItem = testItem.copy(completed = true, completedAt = "2026-10-18T10:05:00Z")
        coEvery { actionItemRepository.toggleCompleted("action-1", true) } returns NetworkResult.Success(updatedItem)

        val viewModel = ActionItemsViewModel(callRepository, actionItemRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleCompleted(testItem)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { actionItemRepository.toggleCompleted("action-1", true) }
        assertTrue(viewModel.uiState.value.actionItems[0].completed)
    }

    @Test
    fun deleteActionItem_removesFromLocalList() = runTest {
        coEvery { actionItemRepository.deleteActionItem("action-1") } returns NetworkResult.Success(true)

        val viewModel = ActionItemsViewModel(callRepository, actionItemRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteActionItem("action-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { actionItemRepository.deleteActionItem("action-1") }
        assertTrue(viewModel.uiState.value.actionItems.isEmpty())
    }
}
