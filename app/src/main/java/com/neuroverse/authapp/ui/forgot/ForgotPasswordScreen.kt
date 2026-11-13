package com.neuroverse.authapp.ui.forgot

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neuroverse.authapp.ui.components.AppTextField
import com.neuroverse.authapp.ui.components.AuthScaffold
import com.neuroverse.authapp.ui.components.BannerType
import com.neuroverse.authapp.ui.components.MessageBanner
import com.neuroverse.authapp.ui.components.PrimaryButton
import com.neuroverse.authapp.ui.theme.NeuroVerseTheme

/**
 * Screen 4 of 6 (spec 3.6) — Forgot Password (spec 3.3).
 *
 * Deliberately does NOT let the user set a new password in the app. The reset
 * link goes to their email, and the new password is set on the provider's own
 * verified page. That is what makes the flow secure: possession of the inbox is
 * the proof of identity. An in-app "enter your new password" form would have
 * nothing to verify against.
 */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(factory = ForgotPasswordViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ForgotPasswordContent(
        uiState = uiState,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::sendResetLink,
        onBack = onBack
    )
}

@Composable
private fun ForgotPasswordContent(
    uiState: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    AuthScaffold(
        title = "Reset password",
        subtitle = "Enter the email address for your account and we will send you a reset link.",
        onBack = onBack
    ) {
        Spacer(Modifier.height(16.dp))

        MessageBanner(message = uiState.formError, type = BannerType.ERROR)
        MessageBanner(message = uiState.successMessage, type = BannerType.SUCCESS)

        Spacer(Modifier.height(8.dp))

        AppTextField(
            value = uiState.email,
            onValueChange = onEmailChange,
            label = "Email address",
            errorMessage = uiState.emailError,
            enabled = !uiState.isLoading,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            testTag = "forgot_email"
        )

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = "Send reset link",
            onClick = onSubmit,
            enabled = uiState.canSubmit,
            isLoading = uiState.isLoading
        )

        Spacer(Modifier.height(8.dp))

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to sign in")
        }
    }
}

@Preview(showBackground = true, name = "Forgot password")
@Composable
private fun ForgotPreview() {
    NeuroVerseTheme { ForgotPasswordContent(ForgotPasswordUiState(), {}, {}, {}) }
}

@Preview(showBackground = true, name = "Forgot password — sent")
@Composable
private fun ForgotSentPreview() {
    NeuroVerseTheme {
        ForgotPasswordContent(
            ForgotPasswordUiState(
                email = "someone@example.com",
                successMessage = "If an account exists for this email, a password " +
                    "reset link has been sent. Please check your inbox."
            ),
            {}, {}, {}
        )
    }
}
