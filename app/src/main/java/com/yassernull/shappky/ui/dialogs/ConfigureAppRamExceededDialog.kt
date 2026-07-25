package com.yassernull.shappky.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.yassernull.shappky.data.models.AppModel
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerRule
import java.util.UUID

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
    containerColor = MaterialTheme.colorScheme.surface,
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
