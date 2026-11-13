package com.neuroverse.authapp.ui

import com.neuroverse.authapp.MainDispatcherRule
import com.neuroverse.authapp.data.model.AuthError
import com.neuroverse.authapp.fake.FakeAuthRepository
import com.neuroverse.authapp.ui.register.RegisterViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Covers spec test scenarios 1 ("Register a new user successfully") and
 * 2 ("Prevent duplicate email registration").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private fun viewModel() = RegisterViewModel(repository)

    /** Fills the form with a valid set of values. */
    private fun RegisterViewModel.fillValidForm(
        name: String = "Prabhakar Kumar",
        email: String = "prabhakar@example.com",
        password: String = "password1"
    ) {
        onNameChange(name)
        onEmailChange(email)
        onPasswordChange(password)
        onConfirmPasswordChange(password)
    }

    // ------------------------------------------- scenario 1: happy path

    @Test
    fun `scenario 1 - registering a new user succeeds`() = runTest {
        val vm = viewModel()
        vm.fillValidForm()

        vm.register()
        advanceUntilIdle()     // let the coroutine run to completion

        val state = vm.uiState.value
        assertTrue("registration should have succeeded", state.isRegistrationSuccessful)
        assertFalse(state.isLoading)
        assertNull(state.formError)
        assertEquals("prabhakar@example.com", repository.currentUser.value?.email)
    }

    @Test
    fun `loading flag is set while the request is in flight and cleared after`() = runTest {
        val vm = viewModel()
        vm.fillValidForm()

        // Hold the repository call open so the in-flight state is observable.
        // Without the gate the fake starts and finishes in one dispatcher step
        // and there is no "while loading" moment to assert on.
        repository.openGate()

        vm.register()
        runCurrent()   // start the coroutine; it now parks inside the gate

        assertTrue("button must be disabled while loading", vm.uiState.value.isLoading)
        assertFalse("canSubmit must be false while loading", vm.uiState.value.canSubmit)

        repository.releaseGate()
        advanceUntilIdle()

        assertFalse("loading must be cleared when done", vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.isRegistrationSuccessful)
    }

    // -------------------------------------- scenario 2: duplicate email

    @Test
    fun `scenario 2 - registering an existing email is rejected`() = runTest {
        repository.seedAccount("Existing User", "taken@example.com", "password1")

        val vm = viewModel()
        vm.fillValidForm(email = "taken@example.com")

        vm.register()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse("must not succeed", state.isRegistrationSuccessful)
        // The error is attached to the EMAIL FIELD, not the form banner, so it
        // points at the field the user has to change.
        assertEquals(
            AuthError.EmailAlreadyRegistered.message,
            state.emailError
        )
    }

    // --------------------------------------- scenario 5: invalid inputs

    @Test
    fun `scenario 5 - invalid email blocks submission without calling the backend`() = runTest {
        val vm = viewModel()
        vm.fillValidForm(email = "not-an-email")

        vm.register()
        advanceUntilIdle()

        assertNotNull("email error expected", vm.uiState.value.emailError)
        assertFalse(vm.uiState.value.isRegistrationSuccessful)
        // The whole point of client validation: no wasted network round trip.
        assertNull("backend must not have been called", repository.currentUser.value)
    }

    @Test
    fun `scenario 5 - weak password blocks submission`() = runTest {
        val vm = viewModel()
        vm.fillValidForm(password = "short")

        vm.register()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.passwordError)
        assertFalse(vm.uiState.value.isRegistrationSuccessful)
    }

    @Test
    fun `mismatched password confirmation blocks submission`() = runTest {
        val vm = viewModel()
        vm.onNameChange("Prabhakar Kumar")
        vm.onEmailChange("prabhakar@example.com")
        vm.onPasswordChange("password1")
        vm.onConfirmPasswordChange("password2")   // does not match

        vm.register()
        advanceUntilIdle()

        assertEquals("Passwords do not match.", vm.uiState.value.confirmPasswordError)
        assertFalse(vm.uiState.value.isRegistrationSuccessful)
    }

    @Test
    fun `all field errors are reported at once, not one at a time`() = runTest {
        val vm = viewModel()
        vm.onNameChange("")
        vm.onEmailChange("bad")
        vm.onPasswordChange("x")
        vm.onConfirmPasswordChange("y")

        vm.register()
        advanceUntilIdle()

        val state = vm.uiState.value
        // A form that reveals one error per submit takes four round trips to fill.
        assertNotNull(state.nameError)
        assertNotNull(state.emailError)
        assertNotNull(state.passwordError)
        assertNotNull(state.confirmPasswordError)
    }

    @Test
    fun `editing a field clears its error`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("bad")
        vm.register()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.emailError)

        vm.onEmailChange("good@example.com")
        assertNull("error must clear as soon as the user starts fixing it",
            vm.uiState.value.emailError)
    }

    // ------------------------------------- scenario 10: network failure

    @Test
    fun `scenario 10 - network failure surfaces a readable error`() = runTest {
        val vm = viewModel()
        vm.fillValidForm()
        repository.nextError = AuthError.Network

        vm.register()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(AuthError.Network.message, state.formError)
        assertFalse(state.isRegistrationSuccessful)
        assertFalse("loading must clear even on failure", state.isLoading)
    }
}
