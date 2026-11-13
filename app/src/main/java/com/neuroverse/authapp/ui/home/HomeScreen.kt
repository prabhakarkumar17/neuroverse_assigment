package com.neuroverse.authapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neuroverse.authapp.data.model.User
import com.neuroverse.authapp.ui.components.PrimaryButton
import com.neuroverse.authapp.ui.theme.NeuroVerseTheme

/**
 * Screen 5 of 6 (spec 3.6) — Home, the landing screen after login.
 *
 * This is a PROTECTED screen (spec 2, 3.4): it can only be reached with a live
 * session. The guard is not here, though — it is in AuthNavGraph, which never
 * routes to Home while signed out. Putting the check in the navigation graph
 * rather than in each screen means a new protected screen is protected by
 * default instead of by remembering to add a check.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: User,
    providerName: String,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    // Confirmation dialog state. rememberSaveable so an accidental rotation
    // mid-dialog does not silently dismiss it.
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    TextButton(onClick = onNavigateToProfile) { Text("Profile") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Greeting ---------------------------------------------------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.initial,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(
                        text = "Hello, ${user.name}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Session card -----------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Session", style = MaterialTheme.typography.titleMedium)
                    InfoRow("Status", "Authenticated")
                    InfoRow("Provider", providerName)
                    InfoRow(
                        "Email verified",
                        if (user.isEmailVerified) "Yes" else "Not verified"
                    )
                    Text(
                        text = "Your session is stored encrypted on this device and is " +
                            "restored automatically when you reopen the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = onNavigateToProfile,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("View profile")
            }

            // Spec 3.5: "Provide a logout button".
            PrimaryButton(
                text = "Log out",
                onClick = { showLogoutDialog = true }
            )
        }
    }

    // Logout is destructive and one tap away, so confirm it. Not in the spec;
    // standard practice for an irreversible action.
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = { Text("You will need to sign in again to access your account.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Log out") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/** One label/value row inside the session card. */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, name = "Home")
@Composable
private fun HomePreview() {
    NeuroVerseTheme {
        HomeScreen(
            user = User("1", "Prabhakar Kumar", "prabhakar@neuroverse.com", false),
            providerName = "Local (offline demo mode)",
            onNavigateToProfile = {},
            onLogout = {}
        )
    }
}
