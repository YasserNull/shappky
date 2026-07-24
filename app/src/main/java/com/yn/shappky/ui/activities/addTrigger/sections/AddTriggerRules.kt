package com.yn.shappky.ui.activities.addTrigger.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yn.shappky.R
import com.yn.shappky.data.models.RuleType
import com.yn.shappky.data.models.TriggerRule
import com.yn.shappky.ui.activities.addTrigger.AddTriggerRulesDialogs
import com.yn.shappky.ui.components.buildRuleSummary
import com.yn.shappky.utils.loadAllApps
import java.util.UUID

@Composable
fun RulesSection(
  rules: List<TriggerRule>,
  onRulesChange: (List<TriggerRule>) -> Unit,
) {
  val context = LocalContext.current
  var showRuleSelection by remember { mutableStateOf(false) }
  var activeConfigType by remember { mutableStateOf<RuleType?>(null) }
  var showAppOpenedPicker by remember { mutableStateOf(false) }
  var showAppResumedPicker by remember { mutableStateOf(false) }
  var showAppClosedPicker by remember { mutableStateOf(false) }
  var showAppKilledPicker by remember { mutableStateOf(false) }

  AddTriggerRulesDialogs(
    showRuleSelection = showRuleSelection,
    onDismissRuleSelection = { showRuleSelection = false },
    onSelectRuleType = { type ->
      showRuleSelection = false
      when (type) {
        RuleType.PHONE_WAKE -> {
          onRulesChange(
            rules + TriggerRule(
              id = UUID.randomUUID().toString(),
              type = RuleType.PHONE_WAKE,
            ),
          )
        }
        RuleType.APP_BACKGROUND_STARTED -> {
          onRulesChange(
            rules + TriggerRule(
              id = UUID.randomUUID().toString(),
              type = RuleType.APP_BACKGROUND_STARTED,
            ),
          )
        }
        RuleType.APP_OPENED -> showAppOpenedPicker = true
        RuleType.APP_RESUMED -> showAppResumedPicker = true
        RuleType.APP_CLOSED -> showAppClosedPicker = true
        RuleType.APP_KILLED_MANUALLY -> showAppKilledPicker = true
        else -> activeConfigType = type
      }
    },
    showAppOpenedPicker = showAppOpenedPicker,
    onDismissAppOpenedPicker = { showAppOpenedPicker = false },
    onSaveAppOpenedPicker = { selected ->
      if (selected.isNotEmpty()) {
        onRulesChange(
          rules + TriggerRule(
            id = UUID.randomUUID().toString(),
            type = RuleType.APP_OPENED,
            appPackages = selected,
          ),
        )
      }
      showAppOpenedPicker = false
    },
    showAppResumedPicker = showAppResumedPicker,
    onDismissAppResumedPicker = { showAppResumedPicker = false },
    onSaveAppResumedPicker = { selected ->
      if (selected.isNotEmpty()) {
        onRulesChange(
          rules + TriggerRule(
            id = UUID.randomUUID().toString(),
            type = RuleType.APP_RESUMED,
            appPackages = selected,
          ),
        )
      }
      showAppResumedPicker = false
    },
    showAppClosedPicker = showAppClosedPicker,
    onDismissAppClosedPicker = { showAppClosedPicker = false },
    onSaveAppClosedPicker = { selected ->
      if (selected.isNotEmpty()) {
        onRulesChange(
          rules + TriggerRule(
            id = UUID.randomUUID().toString(),
            type = RuleType.APP_CLOSED,
            appPackages = selected,
          ),
        )
      }
      showAppClosedPicker = false
    },
    showAppKilledPicker = showAppKilledPicker,
    onDismissAppKilledPicker = { showAppKilledPicker = false },
    onSaveAppKilledPicker = { selected ->
      if (selected.isNotEmpty()) {
        onRulesChange(
          rules + TriggerRule(
            id = UUID.randomUUID().toString(),
            type = RuleType.APP_KILLED_MANUALLY,
            appPackages = selected,
          ),
        )
      }
      showAppKilledPicker = false
    },
    activeConfigType = activeConfigType,
    onDismissActiveConfig = { activeConfigType = null },
    onSaveActiveConfig = { rule ->
      onRulesChange(rules + rule)
      activeConfigType = null
    },
    onSaveTimeConfig = { hour, minute ->
      onRulesChange(
        rules + TriggerRule(
          id = UUID.randomUUID().toString(),
          type = RuleType.SPECIFIC_TIME,
          timeHour = hour,
          timeMinute = minute,
        ),
      )
      activeConfigType = null
    },
    loadAllApps = { callback -> context.loadAllApps(callback) },
  )

  Text(
    text = stringResource(R.string.rules),
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(vertical = 8.dp),
  )

  Button(
    onClick = { showRuleSelection = true },
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(stringResource(R.string.add_rule))
  }

  Spacer(modifier = Modifier.height(16.dp))

  if (rules.isEmpty()) {
    Text(
      text = stringResource(R.string.rules_empty),
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
      fontSize = 14.sp,
      modifier = Modifier.padding(vertical = 8.dp),
    )
  } else {
    rules.forEach { rule ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val ruleIcon = when (rule.type) {
          RuleType.APP_OPENED -> Icons.Filled.Apps
          RuleType.APP_RESUMED -> Icons.Filled.Apps
          RuleType.APP_CLOSED -> Icons.Filled.Apps
          RuleType.APP_KILLED_MANUALLY -> Icons.Filled.Close
          RuleType.RAM_LIMIT_REACHED -> Icons.Filled.Speed
          RuleType.APP_RAM_EXCEEDED -> Icons.Filled.SdStorage
          RuleType.PHONE_SLEEP -> Icons.Filled.PowerSettingsNew
          RuleType.PHONE_WAKE -> Icons.Filled.FlashOn
          RuleType.SPECIFIC_TIME -> Icons.Filled.Schedule
          RuleType.APP_INACTIVITY -> Icons.Filled.Schedule
          RuleType.SERVICE_STATE_CHANGED -> Icons.Filled.Settings
          RuleType.KILL_OLDEST_APP -> Icons.AutoMirrored.Filled.List
          RuleType.APP_BACKGROUND_STARTED -> Icons.Filled.VisibilityOff
        }
        Icon(
          imageVector = ruleIcon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        )
        Spacer(Modifier.width(16.dp))
        Text(
          text = buildRuleSummary(rule),
          fontSize = 15.sp,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        IconButton(
          onClick = { onRulesChange(rules.filter { it.id != rule.id }) },
        ) {
          Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}
