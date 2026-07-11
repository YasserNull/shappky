package com.yn.shappky.ui.activities.addTrigger

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R
import com.yn.shappky.data.models.AppModel
import com.yn.shappky.data.models.RuleType
import com.yn.shappky.data.models.TriggerRule
import com.yn.shappky.ui.dialogs.*

@Composable
fun AddTriggerRulesDialogs(
  showRuleSelection: Boolean,
  onDismissRuleSelection: () -> Unit,
  onSelectRuleType: (RuleType) -> Unit,
  showAppOpenedPicker: Boolean,
  onDismissAppOpenedPicker: () -> Unit,
  onSaveAppOpenedPicker: (Set<String>) -> Unit,
  showAppResumedPicker: Boolean,
  onDismissAppResumedPicker: () -> Unit,
  onSaveAppResumedPicker: (Set<String>) -> Unit,
  showAppClosedPicker: Boolean,
  onDismissAppClosedPicker: () -> Unit,
  onSaveAppClosedPicker: (Set<String>) -> Unit,
  showAppKilledPicker: Boolean,
  onDismissAppKilledPicker: () -> Unit,
  onSaveAppKilledPicker: (Set<String>) -> Unit,
  activeConfigType: RuleType?,
  onDismissActiveConfig: () -> Unit,
  onSaveActiveConfig: (TriggerRule) -> Unit,
  onSaveTimeConfig: (Int, Int) -> Unit,
  loadAllApps: ((List<AppModel>) -> Unit) -> Unit,
) {
  if (showRuleSelection) {
    RuleSelectionDialog(
      onDismiss = onDismissRuleSelection,
      onSelectRuleType = onSelectRuleType,
    )
  }

  if (showAppOpenedPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_opened),
      initialSelectedPackages = emptySet(),
      loadAllApps = loadAllApps,
      onDismiss = onDismissAppOpenedPicker,
      onSave = onSaveAppOpenedPicker,
    )
  }

  if (showAppResumedPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_resumed),
      initialSelectedPackages = emptySet(),
      loadAllApps = loadAllApps,
      onDismiss = onDismissAppResumedPicker,
      onSave = onSaveAppResumedPicker,
    )
  }

  if (showAppClosedPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_closed),
      initialSelectedPackages = emptySet(),
      loadAllApps = loadAllApps,
      onDismiss = onDismissAppClosedPicker,
      onSave = onSaveAppClosedPicker,
    )
  }

  if (showAppKilledPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_killed_manually),
      initialSelectedPackages = emptySet(),
      loadAllApps = loadAllApps,
      onDismiss = onDismissAppKilledPicker,
      onSave = onSaveAppKilledPicker,
    )
  }

  if (activeConfigType == RuleType.RAM_LIMIT_REACHED) {
    ConfigureRamLimitDialog(
      onDismiss = onDismissActiveConfig,
      onConfirm = onSaveActiveConfig,
    )
  }

  if (activeConfigType == RuleType.PHONE_SLEEP) {
    ConfigurePhoneSleepDialog(
      onDismiss = onDismissActiveConfig,
      onConfirm = onSaveActiveConfig,
    )
  }

  if (activeConfigType == RuleType.APP_RAM_EXCEEDED) {
    ConfigureAppRamExceededDialog(
      loadAllApps = loadAllApps,
      onDismiss = onDismissActiveConfig,
      onConfirm = onSaveActiveConfig,
    )
  }

  if (activeConfigType == RuleType.SPECIFIC_TIME) {
    ShowTimePickerDialog(
      onDismiss = onDismissActiveConfig,
      onConfirm = onSaveTimeConfig,
    )
  }

  if (activeConfigType == RuleType.APP_INACTIVITY) {
    ConfigureAppInactivityDialog(
      loadAllApps = loadAllApps,
      onDismiss = onDismissActiveConfig,
      onConfirm = onSaveActiveConfig,
    )
  }

  if (activeConfigType == RuleType.SERVICE_STATE_CHANGED) {
    ConfigureServiceStateDialog(
      onDismiss = onDismissActiveConfig,
      onConfirm = onSaveActiveConfig,
    )
  }

  if (activeConfigType == RuleType.KILL_OLDEST_APP) {
    ConfigureKillOldestAppDialog(
      onDismiss = onDismissActiveConfig,
      onConfirm = onSaveActiveConfig,
    )
  }
}

@Composable
fun AddTriggerSelectAppsDialogs(
  showExcludeDialog: Boolean,
  onDismissExcludeDialog: () -> Unit,
  onSaveExcludeDialog: (Set<String>) -> Unit,
  excludedApps: Set<String>,
  showManualDialog: Boolean,
  onDismissManualDialog: () -> Unit,
  onSaveManualDialog: (Set<String>) -> Unit,
  manuallySelectedApps: Set<String>,
  loadAllApps: ((List<AppModel>) -> Unit) -> Unit,
) {
  if (showExcludeDialog) {
    AppSelectionDialog(
      title = stringResource(R.string.exclude_apps),
      initialSelectedPackages = excludedApps,
      loadAllApps = loadAllApps,
      onDismiss = onDismissExcludeDialog,
      onSave = onSaveExcludeDialog,
    )
  }

  if (showManualDialog) {
    AppSelectionDialog(
      title = stringResource(R.string.manually_select_apps),
      initialSelectedPackages = manuallySelectedApps,
      loadAllApps = loadAllApps,
      onDismiss = onDismissManualDialog,
      onSave = onSaveManualDialog,
    )
  }
}
