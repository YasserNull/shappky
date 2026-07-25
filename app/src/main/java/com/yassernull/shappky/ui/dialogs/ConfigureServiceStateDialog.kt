package com.yassernull.shappky.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerRule
import java.util.UUID

@Composable
fun ConfigureServiceStateDialog(
  onDismiss: () -> Unit,
  onConfirm: (TriggerRule) -> Unit,
) {
  var selectedServices by remember { mutableStateOf<Set<String>>(emptySet()) }
  val services = remember {
    listOf(
      "wifi" to R.string.service_wifi,
      "bluetooth" to R.string.service_bluetooth,
      "mobile_data" to R.string.service_mobile_data,
      "airplane_mode" to R.string.service_airplane_mode,
      "gps" to R.string.service_gps,
      "hotspot" to R.string.service_hotspot,
      "dnd" to R.string.service_dnd,
      "nfc" to R.string.service_nfc,
    )
  }

  AlertDialog(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.rule_service_state)) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp)
          .verticalScroll(rememberScrollState()),
      ) {
        services.forEach { (key, resId) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                selectedServices = if (selectedServices.contains(key)) {
                  selectedServices - key
                } else {
                  selectedServices + key
                }
              }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(resId),
              modifier = Modifier.weight(1f),
              style = MaterialTheme.typography.bodyLarge,
            )
            Checkbox(
              checked = selectedServices.contains(key),
              onCheckedChange = { checked ->
                selectedServices = if (checked) {
                  selectedServices + key
                } else {
                  selectedServices - key
                }
              },
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (selectedServices.isNotEmpty()) {
            onConfirm(
              TriggerRule(
                id = UUID.randomUUID().toString(),
                type = RuleType.SERVICE_STATE_CHANGED,
                selectedServices = selectedServices,
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
