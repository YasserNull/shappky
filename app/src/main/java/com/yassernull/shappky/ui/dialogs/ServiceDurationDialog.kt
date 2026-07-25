package com.yassernull.shappky.ui.dialogs

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R

@Composable
fun ServiceDurationDialog(
  currentDurationMs: Long,
  onDurationSelected: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val options = listOf(
    15000L to R.string.duration_15s,
    30000L to R.string.duration_30s,
    60000L to R.string.duration_1m,
    120000L to R.string.duration_2m,
    300000L to R.string.duration_5m,
    600000L to R.string.duration_10m,
  )

  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.service_duration)) },
    text = {
      Column {
        options.forEach { (durationMs, resId) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onDurationSelected(durationMs) }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = currentDurationMs == durationMs, onClick = null)
            Text(stringResource(resId), modifier = Modifier.padding(start = 8.dp))
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}
