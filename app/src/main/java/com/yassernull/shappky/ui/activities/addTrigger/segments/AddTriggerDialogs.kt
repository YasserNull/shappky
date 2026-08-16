package com.yassernull.shappky.ui.activities.addTrigger

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.AppModel
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerRule
import com.yassernull.shappky.ui.dialogs.*

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
  showAppPausedPicker: Boolean,
  onDismissAppPausedPicker: () -> Unit,
  onSaveAppPausedPicker: (Set<String>) -> Unit,
  showAppExitedPicker: Boolean,
  onDismissAppExitedPicker: () -> Unit,
  onSaveAppExitedPicker: (Set<String>) -> Unit,
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

  if (showAppPausedPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_paused),
      initialSelectedPackages = emptySet(),
      loadAllApps = loadAllApps,
      onDismiss = onDismissAppPausedPicker,
      onSave = onSaveAppPausedPicker,
    )
  }

  if (showAppExitedPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_exited),
      initialSelectedPackages = emptySet(),
      loadAllApps = loadAllApps,
      onDismiss = onDismissAppExitedPicker,
      onSave = onSaveAppExitedPicker,
    )
  }

  if (showAppKilledPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_killed),
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
