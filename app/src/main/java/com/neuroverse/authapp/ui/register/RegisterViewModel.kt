package com.neuroverse.authapp.ui.register

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

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val formError: String? = null,
    val isLoading: Boolean = false,
    val isRegistrationSuccessful: Boolean = false
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank() && confirmPassword.isNotBlank() && !isLoading
}

/** Registration screen logic (spec 3.1). */
class RegisterViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, nameError = null, formError = null) }

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }

    fun onPasswordChange(value: String) =
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                // Re-check the confirmation against the NEW password: editing
                // the first field can make a previously-matching confirmation
                // wrong, and leaving a stale "matches" state would let a
                // mismatched pair through.
                confirmPasswordError =
                    if (it.confirmPassword.isNotEmpty() && it.confirmPassword != value)
                        "Passwords do not match."
                    else null,
                formError = null
            )
        }

    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, formError = null) }

    /** Spec 3.1: validate every field, then create the account. */
    fun register() {
        val state = _uiState.value

        val nameResult = Validators.validateName(state.name)
        val emailResult = Validators.validateEmail(state.email)
        val passwordResult = Validators.validatePassword(state.password)
        val confirmResult =
            Validators.validatePasswordConfirmation(state.password, state.confirmPassword)

        // Validate ALL fields and report all errors at once. Reporting only the
        // first means the user fixes one, resubmits, and is told about the next
        // — four round trips to fill one form.
        val hasError = !nameResult.isValid || !emailResult.isValid ||
            !passwordResult.isValid || !confirmResult.isValid

        if (hasError) {
            _uiState.update {
                it.copy(
                    nameError = nameResult.errorMessage,
                    emailError = emailResult.errorMessage,
                    passwordError = passwordResult.errorMessage,
                    confirmPasswordError = confirmResult.errorMessage
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, formError = null) }

            when (val outcome = repository.register(state.name, state.email, state.password)) {
                is AuthOutcome.Success ->
                    _uiState.update { it.copy(isLoading = false, isRegistrationSuccessful = true) }

                is AuthOutcome.Failure -> {
                    val error = outcome.error
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // Attach a duplicate-email error to the EMAIL FIELD
                            // rather than the form banner: it points at the
                            // field the user has to change (spec 3.1).
                            emailError = if (error is com.neuroverse.authapp.data.model.AuthError.EmailAlreadyRegistered)
                                error.message else null,
                            formError = if (error is com.neuroverse.authapp.data.model.AuthError.EmailAlreadyRegistered)
                                null else error.message
                        )
                    }
                }
            }
        }
    }

    fun onNavigationHandled() =
        _uiState.update { it.copy(isRegistrationSuccessful = false) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                RegisterViewModel(ServiceLocator.authRepository(app))
            }
        }
    }
}
