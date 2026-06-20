package com.yn.shappky.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.yn.shappky.R

@Composable
fun RefreshIntervalDialog(
    title: String,
    currentIntervalMs: Long,
    defaultIntervalMs: Long,
    minIntervalMs: Long,
    onApply: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var intervalText by remember { mutableStateOf(currentIntervalMs.toString()) }
    val intervalMs = intervalText.toLongOrNull()
    val isValid = intervalMs != null && intervalMs >= minIntervalMs

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.refresh_interval_ms)) },
                    supportingText = {
                        if (intervalMs != null && intervalMs < minIntervalMs) {
                            Text(
                                text = stringResource(R.string.min_interval_warning, minIntervalMs),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    isError = intervalMs != null && intervalMs < minIntervalMs,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    if (intervalMs != null) {
                        onApply(intervalMs)
                    }
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { intervalText = defaultIntervalMs.toString() }) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
