package com.yassernull.shappky.ui.activities.serviceCustomization.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerRule
import com.yassernull.shappky.ui.activities.serviceCustomization.ServiceCustomizationRulesDialogs
import com.yassernull.shappky.ui.components.buildRuleSummary
import java.util.UUID

@Composable
fun ServiceCustomizationEnableTriggers(
  enableRules: List<TriggerRule>,
  onEnableRulesChange: (List<TriggerRule>) -> Unit,
) {
  val context = LocalContext.current

  var showRuleSelection by remember { mutableStateOf(false) }
  var activeConfigType by remember { mutableStateOf<RuleType?>(null) }
  var showAppOpenedPicker by remember { mutableStateOf(false) }
  var showAppResumedPicker by remember { mutableStateOf(false) }
  var showAppPausedPicker by remember { mutableStateOf(false) }
  var showAppExitedPicker by remember { mutableStateOf(false) }
  var showAppKilledPicker by remember { mutableStateOf(false) }
  var showAppRamPicker by remember { mutableStateOf(false) }
  var showInactivityPicker by remember { mutableStateOf(false) }

  fun addRule(rule: TriggerRule) {
    onEnableRulesChange(enableRules + rule)
  }

  ServiceCustomizationRulesDialogs(
    context = context,
    showRuleSelection = showRuleSelection,
    onDismissRuleSelection = { showRuleSelection = false },
    onSelectRuleType = { type ->
      showRuleSelection = false
      when (type) {
        RuleType.PHONE_WAKE -> addRule(TriggerRule(id = UUID.randomUUID().toString(), type = RuleType.PHONE_WAKE))
        RuleType.PHONE_SLEEP -> addRule(TriggerRule(id = UUID.randomUUID().toString(), type = RuleType.PHONE_SLEEP))
        RuleType.SPECIFIC_TIME -> activeConfigType = type
        RuleType.RAM_LIMIT_REACHED -> activeConfigType = type
        RuleType.APP_OPENED -> showAppOpenedPicker = true
        RuleType.APP_RESUMED -> showAppResumedPicker = true
        RuleType.APP_PAUSED -> showAppPausedPicker = true
        RuleType.APP_EXITED -> showAppExitedPicker = true
        RuleType.APP_KILLED -> showAppKilledPicker = true
        RuleType.APP_RAM_EXCEEDED -> showAppRamPicker = true
        RuleType.APP_INACTIVITY -> showInactivityPicker = true
        else -> activeConfigType = type
      }
    },
    showAppOpenedPicker = showAppOpenedPicker,
    onDismissAppOpenedPicker = { showAppOpenedPicker = false },
    onAppOpenedSaved = { pkgs ->
      if (pkgs.isNotEmpty()) {
        addRule(TriggerRule(id = UUID.randomUUID().toString(), type = RuleType.APP_OPENED, appPackages = pkgs))
      }
      showAppOpenedPicker = false
    },
    showAppResumedPicker = showAppResumedPicker,
    onDismissAppResumedPicker = { showAppResumedPicker = false },
    onAppResumedSaved = { pkgs ->
      if (pkgs.isNotEmpty()) {
        addRule(TriggerRule(id = UUID.randomUUID().toString(), type = RuleType.APP_RESUMED, appPackages = pkgs))
      }
      showAppResumedPicker = false
    },
    showAppPausedPicker = showAppPausedPicker,
    onDismissAppPausedPicker = { showAppPausedPicker = false },
    onAppPausedSaved = { pkgs ->
      if (pkgs.isNotEmpty()) {
        addRule(TriggerRule(id = UUID.randomUUID().toString(), type = RuleType.APP_PAUSED, appPackages = pkgs))
      }
      showAppPausedPicker = false
    },
    showAppExitedPicker = showAppExitedPicker,
    onDismissAppExitedPicker = { showAppExitedPicker = false },
    onAppExitedSaved = { pkgs ->
      if (pkgs.isNotEmpty()) {
        addRule(TriggerRule(id = UUID.randomUUID().toString(), type = RuleType.APP_EXITED, appPackages = pkgs))
      }
      showAppExitedPicker = false
    },
    showAppKilledPicker = showAppKilledPicker,
    onDismissAppKilledPicker = { showAppKilledPicker = false },
    onAppKilledSaved = { pkgs ->
      if (pkgs.isNotEmpty()) {
        addRule(TriggerRule(id = UUID.randomUUID().toString(), type = RuleType.APP_KILLED, appPackages = pkgs))
      }
      showAppKilledPicker = false
    },
    showAppRamPicker = showAppRamPicker,
    onDismissAppRamPicker = { showAppRamPicker = false },
    onAppRamSaved = { rule ->
      addRule(rule)
      showAppRamPicker = false
    },
    showInactivityPicker = showInactivityPicker,
    onDismissInactivityPicker = { showInactivityPicker = false },
    onInactivitySaved = { rule ->
      addRule(rule)
      showInactivityPicker = false
    },
    activeConfigType = activeConfigType,
    onDismissConfigureServiceState = { activeConfigType = null },
    onConfigureServiceStateConfirmed = { rule ->
      addRule(rule)
      activeConfigType = null
    },
    onDismissConfigureKillOldestApp = { activeConfigType = null },
    onConfigureKillOldestAppConfirmed = { rule ->
      addRule(rule)
      activeConfigType = null
    },
    onSaveTimeConfig = { hour, minute ->
      addRule(TriggerRule(id = UUID.randomUUID().toString(), type = RuleType.SPECIFIC_TIME, timeHour = hour, timeMinute = minute))
      activeConfigType = null
    },
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(R.string.enable_rules_title),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.weight(1f),
    )
    Button(onClick = { showRuleSelection = true }) {
      Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_content_desc))
      Spacer(Modifier.width(4.dp))
      Text(stringResource(R.string.add_enable_rule))
    }
  }

  if (enableRules.isEmpty()) {
    Text(
      text = stringResource(R.string.no_enable_rules),
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
      modifier = Modifier.padding(vertical = 8.dp),
    )
  } else {
    enableRules.forEach { rule ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val ruleName = buildRuleSummary(rule)
        Text(
          text = ruleName,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onEnableRulesChange(enableRules.filter { it.id != rule.id }) }) {
          Icon(
            Icons.Filled.Delete,
            contentDescription = stringResource(R.string.delete_rule_content_desc),
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }

  HorizontalDivider(
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    modifier = Modifier.padding(vertical = 16.dp),
  )
}
