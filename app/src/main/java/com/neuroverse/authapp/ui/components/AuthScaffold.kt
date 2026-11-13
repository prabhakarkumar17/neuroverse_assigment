package com.neuroverse.authapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.neuroverse.authapp.R

/**
 * Shared page chrome for the auth screens: optional back arrow, a title/subtitle
 * header, and a scrollable, keyboard-aware body.
 *
 * TWO THINGS HERE FIX REAL BUGS, not cosmetics:
 *
 * 1. `.imePadding()` — when the soft keyboard opens it covers the bottom of the
 *    screen. Without this the Submit button ends up underneath it and cannot be
 *    tapped at all. This insets the content by the keyboard height.
 *
 * 2. `.verticalScroll()` — the registration form has four fields plus a button.
 *    On a small phone in landscape, with the keyboard up, that does not fit.
 *    Without scrolling the bottom fields are simply unreachable.
 *
 * Both are invisible on a tall emulator in portrait and break instantly on a
 * real device, which is why they are centralised here rather than left to each
 * screen to remember.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // Only render a bar when there is somewhere to go back to.
            if (onBack != null) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                // The drawable is autoMirrored, so it flips
                                // itself in right-to-left locales.
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            content()
        }
    }
}
