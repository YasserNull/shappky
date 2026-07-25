package com.yassernull.shappky.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import com.yassernull.shappky.R

@Composable
fun KillAppRamDialog(
  initialThreshold: Int,
  onConfirm: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  var limitText by remember { mutableStateOf(if (initialThreshold > 0) initialThreshold.toString() else "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.service_kill_app_on_ram_limit)) },
    text = {
      OutlinedTextField(
        value = limitText,
        onValueChange = { limitText = it },
        label = { Text(stringResource(R.string.ram_limit_mb)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          val limit = limitText.toIntOrNull() ?: 0
          if (limit > 0) {
            onConfirm(limit)
          }
        },
      ) {
        Text(stringResource(R.string.ok))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}
