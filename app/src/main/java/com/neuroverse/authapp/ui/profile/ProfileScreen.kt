package com.neuroverse.authapp.ui.profile

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neuroverse.authapp.data.model.User
import com.neuroverse.authapp.ui.components.PrimaryButton
import com.neuroverse.authapp.R
import com.neuroverse.authapp.ui.theme.NeuroVerseTheme

/**
 * Screen 6 of 6 (spec 3.6) — User Profile: "Display of user account details".
 *
 * Read-only by design. Spec section 9 says "Unnecessary features shall not be
 * added without approval", and editing a profile is not in the required
 * feature list, so this screen displays and does not edit. The user id is shown
 * because it is useful when supporting a user, and it is not a secret — unlike
 * a token, which is why no token appears anywhere on this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: User,
    providerName: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- Avatar -----------------------------------------------------
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.initial,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            // --- Account details --------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Account details", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    DetailRow("Name", user.name)
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    DetailRow("Email", user.email)
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    DetailRow(
                        "Email verified",
                        if (user.isEmailVerified) "Verified" else "Not verified"
                    )
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    DetailRow("Provider", providerName)
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    DetailRow("User ID", user.id)
                }
            }

            Spacer(Modifier.height(4.dp))

            PrimaryButton(text = "Log out", onClick = onLogout)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End
        )
    }
}

@Preview(showBackground = true, name = "Profile")
@Composable
private fun ProfilePreview() {
    NeuroVerseTheme {
        ProfileScreen(
            user = User("a1b2c3d4-e5f6", "Prabhakar Kumar", "prabhakar@neuroverse.com", true),
            providerName = "Firebase Authentication",
            onBack = {},
            onLogout = {}
        )
    }
}
