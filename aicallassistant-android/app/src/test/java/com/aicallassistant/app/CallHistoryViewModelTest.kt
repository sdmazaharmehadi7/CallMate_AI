package com.aicallassistant.app

import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.repository.CallRepository
import com.aicallassistant.app.domain.model.CallItem
import com.aicallassistant.app.domain.model.CallStatus
import com.aicallassistant.app.domain.model.CallType
import com.aicallassistant.app.domain.model.UrgencyLevel
import com.aicallassistant.app.ui.viewmodel.CallHistoryViewModel
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
class CallHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val callRepository = mockk<CallRepository>(relaxed = true)

    private val testCall = CallItem(
        id = "c1",
        callerName = "Alice",
        callerNumber = "+919876543210",
        status = CallStatus.COMPLETED,
        callType = CallType.SIMULATED,
        callerReason = "Courier",
        summary = "Delivered package",
        urgency = UrgencyLevel.LOW,
        isImportant = false,
        durationSeconds = 45,
        hasRecording = false,
        createdAt = "2026-10-18T10:00:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery {
            callRepository.getCalls(any(), any(), any(), any(), any())
        } returns NetworkResult.Success(Pair(listOf(testCall), 1))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsCallsSuccessfully() = runTest {
        val viewModel = CallHistoryViewModel(callRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.calls.size)
        assertEquals("c1", state.calls[0].id)
        assertEquals("Alice", state.calls[0].callerName)
    }

    @Test
    fun onSearchQueryChange_triggersReloadWithSearchParam() = runTest {
        val viewModel = CallHistoryViewModel(callRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("Amazon")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            callRepository.getCalls(
                limit = 20,
                offset = 0,
                status = null,
                search = "Amazon",
                importantOnly = null
            )
        }
        assertEquals("Amazon", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun toggleImportant_callsRepositoryAndUpdatesLocalItem() = runTest {
        coEvery {
            callRepository.toggleImportant("c1", true)
        } returns NetworkResult.Success(mockk(relaxed = true))

        val viewModel = CallHistoryViewModel(callRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleImportant(testCall)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { callRepository.toggleImportant("c1", true) }
        assertTrue(viewModel.uiState.value.calls[0].isImportant)
    }
}
