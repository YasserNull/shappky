package com.yassernull.shappky.ui.activities.addTrigger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.ui.activities.addTrigger.sections.RulesSection
import com.yassernull.shappky.ui.activities.addTrigger.sections.SelectAppsSection
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTriggerContent(
  initialTrigger: TriggerModel?,
  triggerCount: Int,
  onSave: (TriggerModel) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current

  var triggerName by remember { mutableStateOf(initialTrigger?.name ?: context.getString(R.string.default_trigger_name, triggerCount + 1)) }
  var selectUserApps by remember { mutableStateOf(initialTrigger?.selectUserApps ?: false) }
  var selectSystemApps by remember { mutableStateOf(initialTrigger?.selectSystemApps ?: false) }
  var selectPersistentApps by remember { mutableStateOf(initialTrigger?.selectPersistentApps ?: false) }
  var excludedApps by remember { mutableStateOf(initialTrigger?.excludedApps ?: emptySet()) }
  var manuallySelectedApps by remember { mutableStateOf(initialTrigger?.manuallySelectedApps ?: emptySet()) }
  var rules by remember { mutableStateOf(initialTrigger?.rules ?: emptyList()) }
  var triggerIsEnabled by remember { mutableStateOf(initialTrigger?.isEnabled ?: true) }

  Scaffold(
    topBar = {
      AddTriggerToolbar(
        isEditing = initialTrigger != null,
        onBackClick = onBack,
        onSaveClick = {
          val trigger = TriggerModel(
            id = initialTrigger?.id ?: UUID.randomUUID().toString(),
            name = triggerName.trim(),
            selectUserApps = selectUserApps,
            selectSystemApps = selectSystemApps,
            selectPersistentApps = selectPersistentApps,
            excludedApps = excludedApps,
            manuallySelectedApps = manuallySelectedApps,
            rules = rules,
            isEnabled = triggerIsEnabled,
          )
          onSave(trigger)
        },
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
    ) {
      OutlinedTextField(
        value = triggerName,
        onValueChange = { triggerName = it },
        label = { Text(stringResource(R.string.trigger_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(24.dp))

      SelectAppsSection(
        selectUserApps = selectUserApps,
        onSelectUserAppsChange = { selectUserApps = it },
        selectSystemApps = selectSystemApps,
        onSelectSystemAppsChange = { selectSystemApps = it },
        excludedApps = excludedApps,
        onExcludedAppsChange = { excludedApps = it },
        manuallySelectedApps = manuallySelectedApps,
        onManuallySelectedAppsChange = { manuallySelectedApps = it },
      )

      HorizontalDivider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
        modifier = Modifier.padding(vertical = 12.dp),
      )

      RulesSection(
        rules = rules,
        onRulesChange = { rules = it },
      )
    }
  }
}
