package com.yassernull.shappky.ui.activities.widgetConfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.TriggerModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigTriggers(
  triggers: List<TriggerModel>,
  selectedTriggerId: String,
  onTriggerSelected: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectableTriggers = triggers.filter { it.rules.isEmpty() }

  Text(
    text = stringResource(R.string.widget_select_trigger),
    fontSize = 14.sp,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.fillMaxWidth(),
  )
  Spacer(Modifier.height(4.dp))

  if (selectableTriggers.isEmpty()) {
    Text(
      text = stringResource(R.string.widget_no_triggers_warning),
      color = MaterialTheme.colorScheme.error,
      fontSize = 14.sp,
      modifier = Modifier.padding(vertical = 8.dp),
    )
  } else {
    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = !expanded },
      modifier = Modifier.fillMaxWidth(),
    ) {
      val selectedTriggerName = selectableTriggers.firstOrNull { it.id == selectedTriggerId }?.name ?: stringResource(R.string.widget_select_trigger)
      OutlinedTextField(
        value = selectedTriggerName,
        onValueChange = {},
        readOnly = true,
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier = Modifier
          .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
          .fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = MaterialTheme.colorScheme.onSurface,
          unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
      ) {
        selectableTriggers.forEach { trigger ->
          DropdownMenuItem(
            text = { Text(trigger.name, color = MaterialTheme.colorScheme.onSurface) },
            onClick = {
              onTriggerSelected(trigger.id)
              expanded = false
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
          )
        }
      }
    }
  }
}
