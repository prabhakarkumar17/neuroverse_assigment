package com.neuroverse.authapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.neuroverse.authapp.ui.theme.successColor

/** Which colour treatment a [MessageBanner] uses. */
enum class BannerType { ERROR, SUCCESS }

/**
 * Inline success/error banner (spec 3.1: "Display appropriate success or error
 * messages"; 3.2: "Display a clear error message when credentials are
 * incorrect").
 *
 * WHY A BANNER AND NOT A SNACKBAR OR A TOAST
 * ------------------------------------------
 * A Snackbar auto-dismisses after a few seconds and a Toast is even shorter.
 * For a login error the user needs to read the message, look back at the form,
 * and fix something — which takes longer than the message survives. An inline
 * banner stays until the next attempt. Toasts also render outside the app on
 * modern Android and are invisible to Compose UI tests.
 *
 * `message == null` collapses the banner with an animation rather than snapping,
 * so the form does not jump as it appears and disappears.
 */
@Composable
fun MessageBanner(
    message: String?,
    type: BannerType,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        // Captured into a local because `message` could in principle change to
        // null while the exit animation is still running.
        val shown = message ?: ""

        val container = when (type) {
            BannerType.ERROR   -> MaterialTheme.colorScheme.errorContainer
            BannerType.SUCCESS -> successColor.copy(alpha = 0.14f)
        }
        val content = when (type) {
            BannerType.ERROR   -> MaterialTheme.colorScheme.onErrorContainer
            BannerType.SUCCESS -> successColor
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(container)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = shown,
                color = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
