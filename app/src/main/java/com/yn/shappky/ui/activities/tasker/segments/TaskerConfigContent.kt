package com.yn.shappky.ui.activities.tasker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yn.shappky.data.models.TriggerModel
import com.yn.shappky.ui.components.ActionOption

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
        title = { Text("Tasker Configuration") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = onSave) {
            Icon(Icons.Filled.Check, contentDescription = "Save")
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
      Text("Select Action:", style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(8.dp))

      ActionOption(
        label = "Start Shappky Service",
        type = "START_SERVICE",
        currentActionType = actionType,
        onActionTypeChange = onActionTypeChange,
      )
      ActionOption(
        label = "Stop Shappky Service",
        type = "STOP_SERVICE",
        currentActionType = actionType,
        onActionTypeChange = onActionTypeChange,
      )
      ActionOption(
        label = "Execute Specific Trigger",
        type = "EXECUTE_TRIGGER",
        currentActionType = actionType,
        onActionTypeChange = onActionTypeChange,
      )

      if (actionType == "EXECUTE_TRIGGER") {
        Spacer(Modifier.height(16.dp))
        Text("Select Trigger:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (availableTriggers.isEmpty()) {
          Text("No triggers available. Please create one in Shappky first.", color = MaterialTheme.colorScheme.error)
        } else {
          availableTriggers.forEach { trigger ->
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
              Text(trigger.name.ifEmpty { "Trigger ${trigger.id.take(4)}" })
            }
          }
        }
      }
    }
  }
}
