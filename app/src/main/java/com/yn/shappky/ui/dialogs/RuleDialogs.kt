package com.yn.shappky.ui.dialogs

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yn.shappky.R
import com.yn.shappky.data.models.AppModel
import com.yn.shappky.data.models.RuleType
import com.yn.shappky.data.models.TriggerRule
import java.util.Calendar
import java.util.UUID

@Composable
fun RuleSelectionDialog(
  onDismiss: () -> Unit,
  onSelectRuleType: (RuleType) -> Unit,
  excludeRuleTypes: Set<RuleType> = emptySet(),
) {
  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.select_condition)) },
    text = {
      val buttonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      val buttonShape = RoundedCornerShape(8.dp)

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
      ) {
        Button(
          onClick = { onSelectRuleType(RuleType.APP_OPENED) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_app_opened))
        }

        Button(
          onClick = { onSelectRuleType(RuleType.APP_RESUMED) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_app_resumed))
        }

        Button(
          onClick = { onSelectRuleType(RuleType.APP_CLOSED) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_app_closed))
        }

        Button(
          onClick = { onSelectRuleType(RuleType.APP_KILLED_MANUALLY) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_app_killed_manually))
        }

        Button(
          onClick = { onSelectRuleType(RuleType.RAM_LIMIT_REACHED) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_ram_limit))
        }

        Button(
          onClick = { onSelectRuleType(RuleType.APP_RAM_EXCEEDED) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_app_ram_exceeded))
        }

        Button(
          onClick = { onSelectRuleType(RuleType.PHONE_SLEEP) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_phone_sleep))
        }

        Button(
          onClick = { onSelectRuleType(RuleType.PHONE_WAKE) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_phone_wake))
        }

        Button(
          onClick = { onSelectRuleType(RuleType.SPECIFIC_TIME) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_specific_time))
        }

        Button(
          onClick = { onSelectRuleType(RuleType.SERVICE_STATE_CHANGED) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_service_state))
        }

        Button(
          onClick = { onSelectRuleType(RuleType.APP_INACTIVITY) },
          colors = buttonColors,
          shape = buttonShape,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
          Text(stringResource(R.string.rule_app_inactivity))
        }

        if (RuleType.KILL_OLDEST_APP !in excludeRuleTypes) {
          Button(
            onClick = { onSelectRuleType(RuleType.KILL_OLDEST_APP) },
            colors = buttonColors,
            shape = buttonShape,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          ) {
            Text(stringResource(R.string.rule_kill_oldest))
          }
        }

        if (RuleType.APP_BACKGROUND_STARTED !in excludeRuleTypes) {
          Button(
            onClick = { onSelectRuleType(RuleType.APP_BACKGROUND_STARTED) },
            colors = buttonColors,
            shape = buttonShape,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          ) {
            Text(stringResource(R.string.rule_auto_background_started))
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}

@Composable
fun ConfigureRamLimitDialog(
  onDismiss: () -> Unit,
  onConfirm: (TriggerRule) -> Unit,
) {
  var ramLimit by remember { mutableStateOf("") }

  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.rule_ram_limit)) },
    text = {
      OutlinedTextField(
        value = ramLimit,
        onValueChange = { ramLimit = it },
        label = { Text(stringResource(R.string.ram_limit_mb)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          val limit = ramLimit.toIntOrNull() ?: 0
          if (limit > 0) {
            onConfirm(
              TriggerRule(
                id = UUID.randomUUID().toString(),
                type = RuleType.RAM_LIMIT_REACHED,
                ramThresholdMb = limit,
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

@Composable
fun ConfigurePhoneSleepDialog(
  onDismiss: () -> Unit,
  onConfirm: (TriggerRule) -> Unit,
) {
  var duration by remember { mutableStateOf("") }

  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.rule_phone_sleep)) },
    text = {
      OutlinedTextField(
        value = duration,
        onValueChange = { duration = it },
        label = { Text(stringResource(R.string.duration_minutes)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
    },
    confirmButton = {
      TextButton(
        onClick = {
          val minutes = duration.toIntOrNull() ?: 0
          if (minutes > 0) {
            onConfirm(
              TriggerRule(
                id = UUID.randomUUID().toString(),
                type = RuleType.PHONE_SLEEP,
                sleepDurationMinutes = minutes,
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

@Composable
fun ConfigureAppRamExceededDialog(
  loadAllApps: ((List<AppModel>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
  onConfirm: (TriggerRule) -> Unit,
) {
  var ramLimit by remember { mutableStateOf("") }
  var selectedApps by remember { mutableStateOf<Set<String>>(emptySet()) }
  var showAppPicker by remember { mutableStateOf(false) }

  if (showAppPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.select_apps),
      initialSelectedPackages = selectedApps,
      loadAllApps = loadAllApps,
      onDismiss = { showAppPicker = false },
      onSave = {
        selectedApps = it
        showAppPicker = false
      },
    )
  }

  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.rule_app_ram_exceeded)) },
    text = {
      Column {
        OutlinedTextField(
          value = ramLimit,
          onValueChange = { ramLimit = it },
          label = { Text(stringResource(R.string.ram_limit_mb)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = { showAppPicker = true },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            if (selectedApps.isEmpty()) {
              stringResource(R.string.select_apps)
            } else {
              "${stringResource(R.string.select_apps)} (${selectedApps.size})"
            },
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val limit = ramLimit.toIntOrNull() ?: 0
          if (limit > 0 && selectedApps.isNotEmpty()) {
            onConfirm(
              TriggerRule(
                id = UUID.randomUUID().toString(),
                type = RuleType.APP_RAM_EXCEEDED,
                appPackages = selectedApps,
                ramThresholdMb = limit,
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

@Composable
fun ShowTimePickerDialog(
  onDismiss: () -> Unit,
  onConfirm: (Int, Int) -> Unit,
) {
  val context = LocalContext.current
  val calendar = Calendar.getInstance()
  val timePickerDialog = remember {
    TimePickerDialog(
      context,
      { _, selectedHour, selectedMinute ->
        onConfirm(selectedHour, selectedMinute)
      },
      calendar.get(Calendar.HOUR_OF_DAY),
      calendar.get(Calendar.MINUTE),
      DateFormat.is24HourFormat(context),
    ).apply {
      setOnCancelListener { onDismiss() }
    }
  }
  timePickerDialog.show()
}

@Composable
fun ConfigureAppInactivityDialog(
  loadAllApps: ((List<AppModel>) -> Unit) -> Unit, // Kept for signature compatibility if needed
  onDismiss: () -> Unit,
  onConfirm: (TriggerRule) -> Unit,
) {
  var duration by remember { mutableStateOf("") }

  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.rule_app_inactivity)) },
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
                type = RuleType.APP_INACTIVITY,
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

@Composable
fun ConfigureKillOldestAppDialog(
  onDismiss: () -> Unit,
  onConfirm: (TriggerRule) -> Unit,
) {
  var duration by remember { mutableStateOf("") }

  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
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
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
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
