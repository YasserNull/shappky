package com.yassernull.shappky.ui.dialogs

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerRule
import java.util.UUID

@Composable
fun ConfigureKillOldestAppDialog(
  onDismiss: () -> Unit,
  onConfirm: (TriggerRule) -> Unit,
) {
  var duration by remember { mutableStateOf("") }

  AlertDialog(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.rule_kill_oldest)) },
    text = {
      Column {
        OutlinedTextField(
          value = duration,
          onValueChange = { duration = it },
          label = { Text(stringResource(R.string.inactivity_duration)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val minutes = duration.toIntOrNull() ?: 0
          if (minutes > 0) {
            onConfirm(
              TriggerRule(
                id = UUID.randomUUID().toString(),
                type = RuleType.KILL_OLDEST_APP,
                appPackages = emptySet(),
                inactivityDurationMinutes = minutes,
              ),
            )
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
