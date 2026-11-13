package com.neuroverse.authapp.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neuroverse.authapp.ui.components.PrimaryButton
import com.neuroverse.authapp.ui.theme.NeuroVerseTheme

/**
 * Screen 1 of 6 (spec 3.6): the entry point.
 *
 * Shown only to signed-OUT users — [com.neuroverse.authapp.ui.navigation.AuthNavGraph]
 * routes straight past it to Home when a session exists.
 *
 * Note this screen is STATELESS: it holds no state and has no ViewModel, only
 * two lambdas out. That makes it trivially previewable and testable, and is the
 * default Compose should aim for — add a ViewModel only when a screen genuinely
 * owns state.
 */
@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Brand mark -----------------------------------------------------
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "N",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "NeuroVerse",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Secure authentication for your organization.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        PrimaryButton(
            text = "Create an account",
            onClick = onNavigateToRegister
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("I already have an account", style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * @Preview renders this in Android Studio's design pane with no emulator and no
 * build-install-launch cycle. Only works on composables that need no live data,
 * which is one more reason to keep screens stateless.
 */
@Preview(showBackground = true, name = "Welcome")
@Composable
private fun WelcomeScreenPreview() {
    NeuroVerseTheme {
        WelcomeScreen(onNavigateToLogin = {}, onNavigateToRegister = {})
    }
}
