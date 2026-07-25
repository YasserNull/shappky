package com.yassernull.shappky.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.RuleType

@Composable
fun RuleSelectionDialog(
  onDismiss: () -> Unit,
  onSelectRuleType: (RuleType) -> Unit,
  excludeRuleTypes: Set<RuleType> = emptySet(),
) {
  AlertDialog(
    containerColor = MaterialTheme.colorScheme.surface,
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
