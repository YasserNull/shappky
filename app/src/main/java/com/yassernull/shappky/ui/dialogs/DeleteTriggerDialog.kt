package com.yassernull.shappky.ui.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.TriggerModel

@Composable
fun DeleteTriggerDialog(
  trigger: TriggerModel,
  onConfirm: (TriggerModel) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.delete_trigger)) },
    text = { Text(stringResource(R.string.delete_trigger_confirm_message)) },
    confirmButton = {
      TextButton(
        onClick = {
          onConfirm(trigger)
        },
      ) {
        Text(stringResource(R.string.delete))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}
