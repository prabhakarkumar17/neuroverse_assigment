package com.neuroverse.authapp.ui.forgot

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

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val formError: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false
) {
    val canSubmit: Boolean get() = email.isNotBlank() && !isLoading
}

/** Password-reset request logic (spec 3.3). */
class ForgotPasswordViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value, emailError = null, formError = null, successMessage = null)
    }

    fun sendResetLink() {
        val state = _uiState.value

        val emailResult = Validators.validateEmail(state.email)
        if (!emailResult.isValid) {
            _uiState.update { it.copy(emailError = emailResult.errorMessage) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, formError = null, successMessage = null) }

            when (val outcome = repository.sendPasswordReset(state.email)) {
                is AuthOutcome.Success ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // Phrased as "if an account exists" ON PURPOSE. A
                            // definite "email sent" would confirm the address is
                            // registered, turning this screen into an account
                            // enumeration oracle. See AuthRepository docs.
                            successMessage = "If an account exists for this email, " +
                                "a password reset link has been sent. Please check your inbox."
                        )
                    }

                is AuthOutcome.Failure ->
                    _uiState.update { it.copy(isLoading = false, formError = outcome.error.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                ForgotPasswordViewModel(ServiceLocator.authRepository(app))
            }
        }
    }
}
