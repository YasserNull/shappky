package com.yn.shappky.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R

@Composable
fun RestartDialog(
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restart_required_title)) },
        text = { Text(stringResource(R.string.restart_required_message)) },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text(stringResource(R.string.restart_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.restart_later))
            }
        },
    )
}
