package com.yassernull.shappky.ui.activities.tasker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.ui.components.ActionOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskerConfigContent(
  actionType: String,
  triggerId: String?,
  availableTriggers: List<TriggerModel>,
  onActionTypeChange: (String) -> Unit,
  onTriggerIdChange: (String?) -> Unit,
  onSave: () -> Unit,
  onBack: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.tasker_configuration)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
          }
        },
        actions = {
          IconButton(onClick = onSave) {
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save))
          }
        },
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp),
    ) {
      Text(stringResource(R.string.select_action), style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(8.dp))

      Text(stringResource(R.string.tasker_section_service), style = MaterialTheme.typography.titleSmall)
      Spacer(Modifier.height(4.dp))
      ActionOption(
        label = stringResource(R.string.start_shappky_service_action),
        type = "START_SERVICE",
        currentActionType = actionType,
        onActionTypeChange = onActionTypeChange,
      )
      ActionOption(
        label = stringResource(R.string.stop_shappky_service_action),
        type = "STOP_SERVICE",
        currentActionType = actionType,
        onActionTypeChange = onActionTypeChange,
      )

      Spacer(Modifier.height(16.dp))
      Text(stringResource(R.string.tasker_section_triggers), style = MaterialTheme.typography.titleSmall)
      Spacer(Modifier.height(4.dp))
      ActionOption(
        label = stringResource(R.string.execute_trigger_action),
        type = "EXECUTE_TRIGGER",
        currentActionType = actionType,
        onActionTypeChange = onActionTypeChange,
      )
      ActionOption(
        label = stringResource(R.string.enable_trigger_action),
        type = "ENABLE_TRIGGER",
        currentActionType = actionType,
        onActionTypeChange = onActionTypeChange,
      )
      ActionOption(
        label = stringResource(R.string.disable_trigger_action),
        type = "DISABLE_TRIGGER",
        currentActionType = actionType,
        onActionTypeChange = onActionTypeChange,
      )

      if (actionType == "EXECUTE_TRIGGER" || actionType == "ENABLE_TRIGGER" || actionType == "DISABLE_TRIGGER") {
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.select_trigger_label), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        val hasRules = { trigger: TriggerModel ->
          trigger.rules.isNotEmpty() || trigger.enableRules.isNotEmpty() || trigger.disableRules.isNotEmpty()
        }
        val selectableTriggers = when (actionType) {
          "EXECUTE_TRIGGER" -> availableTriggers.filter { !hasRules(it) }
          else -> availableTriggers.filter { hasRules(it) }
        }

        if (selectableTriggers.isEmpty()) {
          Text(stringResource(R.string.no_triggers_available), color = MaterialTheme.colorScheme.error)
        } else {
          selectableTriggers.forEach { trigger ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onTriggerIdChange(trigger.id) }
                .padding(vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = (trigger.id == triggerId),
                onClick = { onTriggerIdChange(trigger.id) },
              )
              Spacer(Modifier.width(8.dp))
              Text(trigger.name.ifEmpty { stringResource(R.string.trigger_name_fallback, trigger.id.take(4)) })
            }
          }
        }
      }
    }
  }
}
