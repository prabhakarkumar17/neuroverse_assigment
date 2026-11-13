package com.neuroverse.authapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The app's main call-to-action button, with a built-in loading state.
 *
 * WHY LOADING IS BAKED IN RATHER THAN LEFT TO EACH SCREEN
 * ------------------------------------------------------
 * Every auth action is a network call, and every one of them must disable its
 * button while in flight. Forget that on one screen and a double-tap sends two
 * registration requests — the classic duplicate-account bug. Making `isLoading`
 * a required-by-design part of the button means the button CANNOT be used
 * without handling it: `enabled` already folds in `!isLoading`.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        // The `&& !isLoading` is the whole point: no second tap mid-request.
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(visible = isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(12.dp))
                }
            }
            Text(
                text = if (isLoading) "Please wait…" else text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
