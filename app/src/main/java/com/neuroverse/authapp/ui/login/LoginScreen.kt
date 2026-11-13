package com.neuroverse.authapp.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.neuroverse.authapp.ui.components.AppTextField
import com.neuroverse.authapp.ui.components.BannerType
import com.neuroverse.authapp.ui.components.AuthScaffold
import com.neuroverse.authapp.ui.components.MessageBanner
import com.neuroverse.authapp.ui.components.PasswordTextField
import com.neuroverse.authapp.ui.components.PrimaryButton
import com.neuroverse.authapp.ui.theme.NeuroVerseTheme

/**
 * Screen 3 of 6 (spec 3.6) — Login (spec 3.2).
 *
 * THE STATEFUL/STATELESS SPLIT used throughout this app:
 *  - [LoginScreen] is STATEFUL: it obtains the ViewModel and collects state.
 *  - [LoginContent] is STATELESS: state in, callbacks out, no ViewModel.
 * Only the stateless half can be previewed and UI-tested, and it is the half
 * that holds all the layout. This costs one extra function and buys testability
 * for the entire screen.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
) {
    // collectAsStateWithLifecycle, NOT collectAsState: it stops collecting when
    // the screen goes to the background, instead of keeping the flow (and any
    // network call behind it) alive behind a locked screen.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    /**
     * LaunchedEffect runs a coroutine tied to this composable, re-running only
     * when its key changes. This is the correct place for a one-off side effect
     * like navigation.
     *
     * WHY NOT NAVIGATE DIRECTLY IN THE SUCCESS BRANCH OF THE VIEWMODEL:
     * composables re-run many times per second. Calling navigate() from the
     * composable body would fire on every recomposition and push a dozen copies
     * of Home onto the back stack. Keyed on the success flag, this fires once.
     */
    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            onLoginSuccess()
            viewModel.onNavigationHandled()   // consume the flag so it cannot re-fire
        }
    }

    LoginContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,     // method reference, like Java's this::foo
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::login,
        onNavigateToRegister = onNavigateToRegister,
        onNavigateToForgotPassword = onNavigateToForgotPassword,
        onBack = onBack
    )
}

@Composable
private fun LoginContent(
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onBack: () -> Unit
) {
    AuthScaffold(
        title = "Welcome back",
        subtitle = "Sign in to continue to your account.",
        onBack = onBack
    ) {
        Spacer(Modifier.height(16.dp))

        // Spec 3.2: "Display a clear error message when credentials are incorrect"
        MessageBanner(message = uiState.formError, type = BannerType.ERROR)

        Spacer(Modifier.height(8.dp))

        AppTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = "Email address",
            errorMessage = uiState.emailError,
            enabled = !uiState.isLoading,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            testTag = "login_email"
        )

        Spacer(Modifier.height(16.dp))

        // Spec 3.2: password show/hide lives inside this component.
        PasswordTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "Password",
            errorMessage = uiState.passwordError,
            enabled = !uiState.isLoading,
            imeAction = ImeAction.Done,
            testTag = "login_password"
        )

        // Spec 3.3: "Provide a Forgot Password option on the Login Screen"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onNavigateToForgotPassword,
                enabled = !uiState.isLoading
            ) {
                Text("Forgot password?")
            }
        }

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = "Sign in",
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
                text = "New here?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onNavigateToRegister, enabled = !uiState.isLoading) {
                Text("Create an account")
            }
        }
    }
}

@Preview(showBackground = true, name = "Login — empty")
@Composable
private fun LoginPreview() {
    NeuroVerseTheme {
        LoginContent(LoginUiState(), {}, {}, {}, {}, {}, {})
    }
}

/** Previewing the ERROR state too: this is where layout bugs actually hide. */
@Preview(showBackground = true, name = "Login — error")
@Composable
private fun LoginErrorPreview() {
    NeuroVerseTheme {
        LoginContent(
            LoginUiState(
                email = "someone@example.com",
                password = "wrong",
                formError = "Incorrect email or password."
            ),
            {}, {}, {}, {}, {}, {}
        )
    }
}
