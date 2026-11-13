package com.neuroverse.authapp.ui.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neuroverse.authapp.core.validation.Validators
import com.neuroverse.authapp.ui.components.AppTextField
import com.neuroverse.authapp.ui.components.AuthScaffold
import com.neuroverse.authapp.ui.components.BannerType
import com.neuroverse.authapp.ui.components.MessageBanner
import com.neuroverse.authapp.ui.components.PasswordTextField
import com.neuroverse.authapp.ui.components.PrimaryButton
import com.neuroverse.authapp.ui.theme.NeuroVerseTheme

/**
 * Screen 2 of 6 (spec 3.6) — Registration (spec 3.1).
 *
 * Collects name, email and password exactly as spec 3.1 requires, plus a
 * confirmation field. The confirmation is not in the spec; it is standard
 * practice for a password that is masked while typing, and prevents an
 * account being created with a typo that the user can then never log in with.
 */
@Composable
fun RegisterScreen(
    onRegistrationSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isRegistrationSuccessful) {
        if (uiState.isRegistrationSuccessful) {
            onRegistrationSuccess()
            viewModel.onNavigationHandled()
        }
    }

    RegisterContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onSubmit = viewModel::register,
        onNavigateToLogin = onNavigateToLogin,
        onBack = onBack
    )
}

@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit
) {
    AuthScaffold(
        title = "Create account",
        subtitle = "Join NeuroVerse in a few seconds.",
        onBack = onBack
    ) {
        Spacer(Modifier.height(16.dp))

        MessageBanner(message = uiState.formError, type = BannerType.ERROR)

        Spacer(Modifier.height(8.dp))

        AppTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = "Full name",
            errorMessage = uiState.nameError,
            enabled = !uiState.isLoading,
            keyboardType = KeyboardType.Text,
            testTag = "register_name"
        )

        Spacer(Modifier.height(16.dp))

        AppTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = "Email address",
            errorMessage = uiState.emailError,
            enabled = !uiState.isLoading,
            keyboardType = KeyboardType.Email,
            testTag = "register_email"
        )

        Spacer(Modifier.height(16.dp))

        PasswordTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "Password",
            errorMessage = uiState.passwordError,
            enabled = !uiState.isLoading,
            imeAction = ImeAction.Next,
            testTag = "register_password"
        )

        // Tells the user the rule UP FRONT instead of rejecting them after they
        // submit. Hidden once an actual error is showing, so the two never
        // stack and contradict each other.
        if (uiState.passwordError == null) {
            Text(
                text = "At least ${Validators.MIN_PASSWORD_LENGTH} characters, " +
                    "including a letter and a number.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        PasswordTextField(
            value = uiState.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm password",
            errorMessage = uiState.confirmPasswordError,
            enabled = !uiState.isLoading,
            imeAction = ImeAction.Done,
            testTag = "register_confirm_password"
        )

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = "Create account",
            onClick = onSubmit,
            enabled = uiState.canSubmit,
            isLoading = uiState.isLoading
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already registered?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onNavigateToLogin, enabled = !uiState.isLoading) {
                Text("Sign in")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, name = "Register — empty")
@Composable
private fun RegisterPreview() {
    NeuroVerseTheme { RegisterContent(RegisterUiState(), {}, {}, {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, name = "Register — duplicate email")
@Composable
private fun RegisterDuplicatePreview() {
    NeuroVerseTheme {
        RegisterContent(
            RegisterUiState(
                name = "Prabhakar Kumar",
                email = "taken@example.com",
                password = "password123",
                confirmPassword = "password123",
                emailError = "An account with this email already exists."
            ),
            {}, {}, {}, {}, {}, {}, {}
        )
    }
}
