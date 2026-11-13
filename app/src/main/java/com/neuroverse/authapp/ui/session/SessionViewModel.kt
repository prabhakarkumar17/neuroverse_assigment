package com.neuroverse.authapp.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.neuroverse.authapp.data.model.User
import com.neuroverse.authapp.data.repository.AuthRepository
import com.neuroverse.authapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-wide authentication state. Owns the answer to "is anyone logged in?" and
 * therefore drives all routing and screen protection (spec 3.4).
 *
 * Scoped to the ACTIVITY, not to a screen, so every destination observes the
 * same instance. If each screen had its own, a logout on Profile would not be
 * seen by Home.
 */
class SessionViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    /**
     * Startup states. Until [restoreSession] finishes we genuinely do not know
     * whether the user is signed in, and [Loading] represents exactly that.
     *
     * WHY THIS MATTERS: without a Loading state the app must assume one or the
     * other. Assume "logged out" and a returning user sees the Login screen
     * flash before being bounced to Home. Modelling "don't know yet" as its own
     * state removes that flicker by construction.
     */
    sealed interface AuthState {
        data object Loading : AuthState
        data object SignedOut : AuthState
        data class SignedIn(val user: User) : AuthState
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /**
     * `init` is the Kotlin initializer block, equivalent to code in a Java
     * constructor. Restoring the session here means it starts the moment the
     * ViewModel is created, before the first screen composes.
     */
    init {
        restoreSession()
        observeRepository()
    }

    /** Spec 3.4: "Restore the user's authenticated state when the application restarts". */
    private fun restoreSession() {
        // viewModelScope is cancelled automatically when the ViewModel is
        // cleared, so this coroutine can never outlive its owner and leak.
        viewModelScope.launch {
            val user = repository.restoreSession()
            _authState.value =
                if (user != null) AuthState.SignedIn(user) else AuthState.SignedOut
        }
    }

    /**
     * Mirrors the repository's own user flow into [authState].
     *
     * This is what makes login and logout propagate without any screen telling
     * another screen anything: the repository is the single source of truth,
     * the UI just follows it.
     */
    private fun observeRepository() {
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                // Ignore nulls while still Loading, or we would overwrite
                // "don't know yet" with "signed out" before restore finishes
                // and reintroduce the login-screen flicker.
                if (user != null) {
                    _authState.value = AuthState.SignedIn(user)
                } else if (_authState.value !is AuthState.Loading) {
                    _authState.value = AuthState.SignedOut
                }
            }
        }
    }

    /** Spec 3.5: logout. Clears provider state and local session. */
    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.SignedOut
        }
    }

    /** Backend name, surfaced on the Home screen so the active mode is obvious. */
    val providerName: String get() = repository.providerName

    companion object {
        /**
         * Factory that builds this ViewModel with its dependency.
         *
         * A ViewModel with constructor parameters cannot be created by the
         * default factory, which only knows how to call a no-arg constructor.
         * The `viewModelFactory { initializer { } }` DSL is the modern
         * replacement for implementing ViewModelProvider.Factory by hand.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                SessionViewModel(ServiceLocator.authRepository(app))
            }
        }
    }
}
