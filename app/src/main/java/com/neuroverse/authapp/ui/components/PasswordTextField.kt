package com.neuroverse.authapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.neuroverse.authapp.R

/**
 * Password field with a show/hide toggle (spec 3.2: "Include a password
 * show/hide option").
 *
 * HOW THE TOGGLE WORKS
 * --------------------
 * The text is ALWAYS the real password in state. Only its *rendering* changes:
 * [PasswordVisualTransformation] draws a bullet per character without touching
 * the underlying value. That matters — a naive implementation that swapped the
 * stored string for asterisks would corrupt the password on submit.
 *
 * WHY `rememberSaveable` FOR THE TOGGLE
 * -------------------------------------
 * `remember` survives recomposition but NOT configuration change.
 * `rememberSaveable` writes to the saved-instance-state Bundle, so the toggle
 * keeps its position when the device rotates. Note the password VALUE is
 * hoisted to the ViewModel, so it is the caller's concern, not this field's.
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    testTag: String? = null
) {
    // Local UI state: nobody outside this field needs to know it.
    var visible by rememberSaveable { mutableStateOf(false) }
    val isError = errorMessage != null

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isError,
            enabled = enabled,
            singleLine = true,
            visualTransformation =
                if (visible) VisualTransformation.None
                else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                // KeyboardType.Password also tells the OS not to offer this
                // text to autocorrect or the clipboard-history suggestion strip.
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            trailingIcon = {
                IconButton(
                    onClick = { visible = !visible },
                    enabled = enabled
                ) {
                    Icon(
                        painter = painterResource(
                            if (visible) R.drawable.ic_visibility_off
                            else R.drawable.ic_visibility
                        ),
                        // contentDescription is read aloud by TalkBack. Never
                        // null on an interactive control, or the field is
                        // unusable for a blind user.
                        contentDescription = if (visible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

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
