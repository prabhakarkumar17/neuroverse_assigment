package com.neuroverse.authapp.ui

import com.neuroverse.authapp.MainDispatcherRule
import com.neuroverse.authapp.data.model.AuthError
import com.neuroverse.authapp.fake.FakeAuthRepository
import com.neuroverse.authapp.ui.login.LoginViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Covers spec test scenarios 3 ("Log in with valid credentials"),
 * 4 ("Reject invalid credentials") and 10 ("Handle network errors").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private fun viewModel() = LoginViewModel(repository)

    @Before
    fun seedRegisteredUser() {
        repository.seedAccount("Prabhakar Kumar", "prabhakar@example.com", "password1")
    }

    // ------------------------------------------- scenario 3: valid login

    @Test
    fun `scenario 3 - login with valid credentials succeeds`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("prabhakar@example.com")
        vm.onPasswordChange("password1")

        vm.login()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isLoginSuccessful)
        assertNull(state.formError)
        assertEquals("prabhakar@example.com", repository.currentUser.value?.email)
    }

    @Test
    fun `email is matched case-insensitively`() = runTest {
        val vm = viewModel()
        // Users type their email however their keyboard auto-capitalises it.
        vm.onEmailChange("Prabhakar@Example.COM")
        vm.onPasswordChange("password1")

        vm.login()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isLoginSuccessful)
    }

    // ---------------------------------------- scenario 4: bad credentials

    @Test
    fun `scenario 4 - wrong password is rejected`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("prabhakar@example.com")
        vm.onPasswordChange("wrongpassword")

        vm.login()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoginSuccessful)
        assertEquals(AuthError.InvalidCredentials.message, state.formError)
    }

    @Test
    fun `scenario 4 - unknown email is rejected with the SAME message as a wrong password`() =
        runTest {
            val vm = viewModel()
            vm.onEmailChange("nobody@example.com")
            vm.onPasswordChange("password1")

            vm.login()
            advanceUntilIdle()

            // SECURITY ASSERTION, not a duplicate of the test above. If these
            // two messages ever differ, the login screen becomes a tool for
            // discovering which emails are registered. This test fails if
            // someone "helpfully" adds a distinct "no such account" message.
            assertEquals(AuthError.InvalidCredentials.message, vm.uiState.value.formError)
        }

    // ----------------------------------------- scenario 5: input errors

    @Test
    fun `scenario 5 - malformed email is caught before any backend call`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("not-an-email")
        vm.onPasswordChange("password1")

        vm.login()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.emailError)
        assertFalse(vm.uiState.value.isLoginSuccessful)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun `empty password is caught before any backend call`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("prabhakar@example.com")
        vm.onPasswordChange("")

        vm.login()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.passwordError)
        assertNull(repository.currentUser.value)
    }

    @Test
    fun `a short password is NOT blocked at login`() = runTest {
        // Registration requires 8+ chars, but login must not: an account made
        // before the rule tightened still has to be able to sign in, and a
        // format rejection would leak that the guess was never even checked.
        val vm = viewModel()
        vm.onEmailChange("prabhakar@example.com")
        vm.onPasswordChange("ab")

        vm.login()
        advanceUntilIdle()

        assertNull("login must not apply registration strength rules",
            vm.uiState.value.passwordError)
        // It fails on credentials instead, which is correct.
        assertEquals(AuthError.InvalidCredentials.message, vm.uiState.value.formError)
    }

    // ----------------------------------------------- scenario 10: network

    @Test
    fun `scenario 10 - network error is reported and loading is cleared`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("prabhakar@example.com")
        vm.onPasswordChange("password1")
        repository.nextError = AuthError.Network

        vm.login()
        advanceUntilIdle()

        assertEquals(AuthError.Network.message, vm.uiState.value.formError)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `rate limiting is reported distinctly`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("prabhakar@example.com")
        vm.onPasswordChange("password1")
        repository.nextError = AuthError.TooManyRequests

        vm.login()
        advanceUntilIdle()

        assertEquals(AuthError.TooManyRequests.message, vm.uiState.value.formError)
    }

    // ------------------------------------------------------- UI contract

    @Test
    fun `submit is disabled until both fields have content`() {
        val vm = viewModel()
        assertFalse(vm.uiState.value.canSubmit)

        vm.onEmailChange("prabhakar@example.com")
        assertFalse("still needs a password", vm.uiState.value.canSubmit)

        vm.onPasswordChange("password1")
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `navigation flag is consumed so it cannot fire twice`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("prabhakar@example.com")
        vm.onPasswordChange("password1")

        vm.login()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoginSuccessful)

        // Without this reset, every recomposition would re-trigger navigation
        // and stack duplicate Home screens.
        vm.onNavigationHandled()
        assertFalse(vm.uiState.value.isLoginSuccessful)
    }
}
