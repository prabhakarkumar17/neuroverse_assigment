package com.neuroverse.authapp.ui

import com.neuroverse.authapp.MainDispatcherRule
import com.neuroverse.authapp.data.model.AuthError
import com.neuroverse.authapp.fake.FakeAuthRepository
import com.neuroverse.authapp.ui.forgot.ForgotPasswordViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Covers spec test scenario 6: "Complete the password-reset flow". */
@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private fun viewModel() = ForgotPasswordViewModel(repository)

    @Test
    fun `scenario 6 - requesting a reset for a known email reports success`() = runTest {
        repository.seedAccount("Prabhakar Kumar", "prabhakar@example.com", "password1")

        val vm = viewModel()
        vm.onEmailChange("prabhakar@example.com")

        vm.sendResetLink()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.successMessage)
        assertNull(vm.uiState.value.formError)
        assertEquals(1, repository.passwordResetRequests)
    }

    @Test
    fun `scenario 6 - an UNKNOWN email reports the same success message`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("nobody@example.com")

        vm.sendResetLink()
        advanceUntilIdle()

        // SECURITY ASSERTION: if this screen said "no such account", anyone
        // could use it to discover which addresses are registered. The response
        // must be identical either way.
        assertNotNull(vm.uiState.value.successMessage)
        assertNull(vm.uiState.value.formError)
    }

    @Test
    fun `success message is deliberately non-committal about whether mail was sent`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("someone@example.com")

        vm.sendResetLink()
        advanceUntilIdle()

        val message = vm.uiState.value.successMessage.orEmpty()
        // Guards the wording itself. "We sent you an email" would confirm the
        // account exists even if the status code does not.
        assertEquals(
            "message should be conditional, was: '$message'",
            true,
            message.startsWith("If an account exists")
        )
    }

    @Test
    fun `invalid email is caught before any backend call`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("not-an-email")

        vm.sendResetLink()
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.emailError)
        assertEquals(0, repository.passwordResetRequests)
    }

    @Test
    fun `network failure is reported`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("someone@example.com")
        repository.nextError = AuthError.Network

        vm.sendResetLink()
        advanceUntilIdle()

        assertEquals(AuthError.Network.message, vm.uiState.value.formError)
        assertNull(vm.uiState.value.successMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `editing the email clears a previous success message`() = runTest {
        val vm = viewModel()
        vm.onEmailChange("someone@example.com")
        vm.sendResetLink()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.successMessage)

        vm.onEmailChange("other@example.com")
        // Otherwise the banner would claim a link was sent to the NEW address.
        assertNull(vm.uiState.value.successMessage)
    }
}
