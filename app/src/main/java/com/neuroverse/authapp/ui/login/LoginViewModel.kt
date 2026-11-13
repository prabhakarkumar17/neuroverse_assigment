package com.neuroverse.authapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.neuroverse.authapp.core.validation.Validators
import com.neuroverse.authapp.data.model.AuthOutcome
import com.neuroverse.authapp.data.repository.AuthRepository
import com.neuroverse.authapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything the Login screen shows, in one immutable snapshot.
 *
 * WHY ONE STATE CLASS INSTEAD OF SIX SEPARATE FLOWS
 * ------------------------------------------------
 * With separate flows for email, password, isLoading and error, the UI can
 * briefly observe an impossible combination — isLoading true AND a success
 * message, say — because each flow updates independently. One immutable object
 * swapped atomically can only ever represent a state that actually happened.
 *
 * `data class` gives us `copy()`, which is how an immutable state gets
 * "modified": `state.copy(email = new)` returns a new object sharing the rest.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val formError: String? = null,
    val isLoading: Boolean = false,
    val isLoginSuccessful: Boolean = false
) {
    /** The button is enabled only when both fields have content. */
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isLoading
}

/**
 * Login screen logic (spec 3.2).
 *
 * THE VIEWMODEL BOUNDARY: this class knows nothing about Compose, Views,
 * Activities or Firebase. It exposes state and accepts events. That is why it
 * survives rotation (Android keeps the ViewModel across the Activity being
 * destroyed and recreated) and why it can be unit-tested with a fake repository
 * and no device.
 */
class LoginViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Clears the field error as soon as the user edits it.
     *
     * Errors appear on submit, not on every keystroke: flagging "invalid email"
     * while someone is still typing the second character is hostile. But once
     * flagged, the error must vanish the moment they start fixing it.
     */
    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, formError = null) }
    }

    /** Spec 3.2: validate, then authenticate. */
    fun login() {
        val state = _uiState.value

        // --- Client-side validation first: no pointless network round trip ---
        val emailResult = Validators.validateEmail(state.email)
        // Presence only, NOT the registration strength rules. See Validators.
        val passwordResult = Validators.validatePasswordPresence(state.password)

        if (!emailResult.isValid || !passwordResult.isValid) {
            _uiState.update {
                it.copy(
                    emailError = emailResult.errorMessage,
                    passwordError = passwordResult.errorMessage
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, formError = null) }

            when (val outcome = repository.login(state.email, state.password)) {
                is AuthOutcome.Success ->
                    // Only flip the success flag. Navigation is the UI's job,
                    // triggered by this flag — a ViewModel that navigated
                    // itself would need a NavController and stop being testable.
                    _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }

                is AuthOutcome.Failure ->
                    _uiState.update {
                        it.copy(isLoading = false, formError = outcome.error.message)
                    }
            }
        }
    }

    /** Called after the UI has consumed [LoginUiState.isLoginSuccessful]. */
    fun onNavigationHandled() {
        _uiState.update { it.copy(isLoginSuccessful = false) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                LoginViewModel(ServiceLocator.authRepository(app))
            }
        }
    }
}
