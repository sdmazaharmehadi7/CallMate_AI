package com.aicallassistant.app

import com.aicallassistant.app.data.auth.AuthRepository
import com.aicallassistant.app.ui.viewmodel.AuthUiState
import com.aicallassistant.app.ui.viewmodel.AuthViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val sessionFlow = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated(false))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.sessionState } returns sessionFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signIn_withInvalidEmail_setsErrorState() = runTest {
        val viewModel = AuthViewModel(authRepository)
        viewModel.signIn("notanemail", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertTrue((state as AuthUiState.Error).message.contains("valid email", ignoreCase = true))
    }

    @Test
    fun signIn_withShortPassword_setsErrorState() = runTest {
        val viewModel = AuthViewModel(authRepository)
        viewModel.signIn("user@example.com", "123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertTrue((state as AuthUiState.Error).message.contains("at least 6 characters", ignoreCase = true))
    }

    @Test
    fun signUp_withMismatchedPasswords_setsErrorState() = runTest {
        val viewModel = AuthViewModel(authRepository)
        viewModel.signUp("user@example.com", "secret123", "secret456")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertTrue((state as AuthUiState.Error).message.contains("do not match", ignoreCase = true))
    }

    @Test
    fun signIn_withValidCredentials_callsRepository() = runTest {
        coEvery { authRepository.signIn("user@example.com", "secret123") } returns Result.success(Unit)
        val viewModel = AuthViewModel(authRepository)

        viewModel.signIn("user@example.com", "secret123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.signIn("user@example.com", "secret123") }
    }

    @Test
    fun signIn_whenRepositoryFails_setsErrorState() = runTest {
        coEvery {
            authRepository.signIn("user@example.com", "wrongpass")
        } returns Result.failure(Exception("Invalid login credentials"))

        val viewModel = AuthViewModel(authRepository)
        viewModel.signIn("user@example.com", "wrongpass")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("Invalid login credentials", (state as AuthUiState.Error).message)
    }
}
