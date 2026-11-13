package com.neuroverse.authapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/**
 * A labelled text field with inline error text underneath.
 *
 * WHY A SHARED COMPONENT: the registration screen alone has four of these. If
 * each screen styled its own, the error text would drift out of alignment the
 * first time someone tweaked one. Compose makes extracting a component a
 * function call, so there is no excuse not to.
 *
 * @param errorMessage null = valid. Non-null switches the field to its error
 *   colours AND renders the message. One parameter drives both, so they cannot
 *   disagree.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    testTag: String? = null
) {
    val isError = errorMessage != null

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isError,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(
                // Shows the user the right keyboard: an @ key for email,
                // digits for numbers. Free usability for one parameter.
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    // testTag gives instrumented tests a stable handle that does
                    // not break when the label text is reworded.
                    if (testTag != null) Modifier.semantics { this.testTag = testTag }
                    else Modifier
                )
        )

        // `?.let` renders this block only when errorMessage is non-null — the
        // Kotlin replacement for `if (msg != null) { ... }`.
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
