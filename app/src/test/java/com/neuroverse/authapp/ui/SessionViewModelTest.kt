package com.neuroverse.authapp.ui

import com.neuroverse.authapp.MainDispatcherRule
import com.neuroverse.authapp.data.model.User
import com.neuroverse.authapp.fake.FakeAuthRepository
import com.neuroverse.authapp.ui.session.SessionViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Covers spec test scenarios 7 ("Maintain the session after restarting the
 * application"), 8 ("Prevent unauthenticated users from accessing protected
 * screens") and 9 ("Log out successfully and clear the local session").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    private val sampleUser = User(
        id = "id-1",
        name = "Prabhakar Kumar",
        email = "prabhakar@example.com"
    )

    // -------------------------------------- scenario 7: session restore

    @Test
    fun `scenario 7 - a saved session is restored on startup`() = runTest {
        // Simulates the app being killed and reopened with a stored session.
        repository.simulateSavedSession(sampleUser)

        val vm = SessionViewModel(repository)
        advanceUntilIdle()

        val state = vm.authState.value
        assertTrue("expected SignedIn, was $state", state is SessionViewModel.AuthState.SignedIn)
        assertEquals(sampleUser, (state as SessionViewModel.AuthState.SignedIn).user)
    }

    @Test
    fun `scenario 8 - with no saved session the state is SignedOut`() = runTest {
        val vm = SessionViewModel(repository)
        advanceUntilIdle()

        // AuthNavGraph routes SignedOut to Welcome/Login, which is what keeps
        // Home and Profile unreachable without a session.
        assertTrue(vm.authState.value is SessionViewModel.AuthState.SignedOut)
    }

    @Test
    fun `state starts as Loading, never as SignedOut`() {
        // Deliberately does NOT advance the dispatcher, so this captures the
        // state at the very first frame.
        val vm = SessionViewModel(repository)

        // If this were SignedOut, a returning user would see the Login screen
        // flash before being redirected to Home. Modelling "don't know yet" as
        // its own state is what removes that flicker.
        assertTrue(
            "startup state must be Loading, was ${vm.authState.value}",
            vm.authState.value is SessionViewModel.AuthState.Loading
        )
    }

    // --------------------------------------------- scenario 9: logout

    @Test
    fun `scenario 9 - logout clears the session and signs the user out`() = runTest {
        repository.simulateSavedSession(sampleUser)
        val vm = SessionViewModel(repository)
        advanceUntilIdle()
        assertTrue(vm.authState.value is SessionViewModel.AuthState.SignedIn)

        vm.logout()
        advanceUntilIdle()

        assertTrue(vm.authState.value is SessionViewModel.AuthState.SignedOut)
        assertEquals("repository must have no current user", null, repository.currentUser.value)
    }

    // ---------------------------------------------------- propagation

    @Test
    fun `signing in anywhere propagates to the session state`() = runTest {
        val vm = SessionViewModel(repository)
        advanceUntilIdle()
        assertTrue(vm.authState.value is SessionViewModel.AuthState.SignedOut)

        // Simulates the LoginViewModel succeeding on another screen. No screen
        // notifies another: both observe the repository.
        repository.seedAccount("Prabhakar Kumar", "prabhakar@example.com", "password1")
        repository.login("prabhakar@example.com", "password1")
        advanceUntilIdle()

        assertTrue(vm.authState.value is SessionViewModel.AuthState.SignedIn)
    }
}
