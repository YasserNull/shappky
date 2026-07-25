package com.yassernull.shappky.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R

@Composable
fun RestartDialog(
  onRestart: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
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
