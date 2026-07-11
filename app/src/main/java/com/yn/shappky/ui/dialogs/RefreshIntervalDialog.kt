package com.yn.shappky.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yn.shappky.R

@Composable
fun RefreshIntervalDialog(
  title: String,
  currentIntervalMs: Long,
  choices: List<Long>,
  onApply: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedInterval by remember { mutableStateOf(currentIntervalMs) }

  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column {
        choices.forEach { choice ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedInterval = choice }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(
              selected = selectedInterval == choice,
              onClick = null,
            )
            Text(
              text = formatInterval(choice),
              modifier = Modifier.padding(start = 8.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onApply(selectedInterval) }) {
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

@Composable
fun formatInterval(ms: Long): String = when {
  ms >= 60000L && ms % 60000L == 0L -> {
    stringResource(R.string.refresh_interval_min, (ms / 60000L).toInt())
  }
  ms >= 1000L && ms % 1000L == 0L -> {
    stringResource(R.string.refresh_interval_sec, (ms / 1000L).toInt())
  }
  else -> {
    stringResource(R.string.refresh_interval_value, ms.toInt())
  }
}
