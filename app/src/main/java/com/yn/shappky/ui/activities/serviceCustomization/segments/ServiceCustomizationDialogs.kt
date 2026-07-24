package com.yn.shappky.ui.activities.serviceCustomization

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R
import com.yn.shappky.data.models.RuleType
import com.yn.shappky.data.models.TriggerRule
import com.yn.shappky.ui.dialogs.*
import com.yn.shappky.utils.loadAllApps

@Composable
fun ServiceCustomizationConfigDialogs(
  showDurationDialog: Boolean,
  onDismissDurationDialog: () -> Unit,
  onServiceDurationSelected: (Long) -> Unit,
  serviceDuration: Long,
  showKillAllRamDialog: Boolean,
  onDismissKillAllRamDialog: () -> Unit,
  onKillAllRamConfirmed: (Int) -> Unit,
  killAllRamThreshold: Int,
  showKillAppRamDialog: Boolean,
  onDismissKillAppRamDialog: () -> Unit,
  onKillAppRamConfirmed: (Int) -> Unit,
  killAppRamThreshold: Int,
) {
  if (showDurationDialog) {
    ServiceDurationDialog(
      currentDurationMs = serviceDuration,
      onDurationSelected = onServiceDurationSelected,
      onDismiss = onDismissDurationDialog,
    )
  }

  if (showKillAllRamDialog) {
    KillAllRamDialog(
      initialThreshold = killAllRamThreshold,
      onConfirm = onKillAllRamConfirmed,
      onDismiss = onDismissKillAllRamDialog,
    )
  }

  if (showKillAppRamDialog) {
    KillAppRamDialog(
      initialThreshold = killAppRamThreshold,
      onConfirm = onKillAppRamConfirmed,
      onDismiss = onDismissKillAppRamDialog,
    )
  }
}

@Composable
fun ServiceCustomizationRulesDialogs(
  context: Context,
  showRuleSelection: Boolean,
  onDismissRuleSelection: () -> Unit,
  onSelectRuleType: (RuleType) -> Unit,
  showAppOpenedPicker: Boolean,
  onDismissAppOpenedPicker: () -> Unit,
  onAppOpenedSaved: (Set<String>) -> Unit,
  showAppResumedPicker: Boolean,
  onDismissAppResumedPicker: () -> Unit,
  onAppResumedSaved: (Set<String>) -> Unit,
  showAppClosedPicker: Boolean,
  onDismissAppClosedPicker: () -> Unit,
  onAppClosedSaved: (Set<String>) -> Unit,
  showAppKilledPicker: Boolean,
  onDismissAppKilledPicker: () -> Unit,
  onAppKilledSaved: (Set<String>) -> Unit,
  showAppRamPicker: Boolean,
  onDismissAppRamPicker: () -> Unit,
  onAppRamSaved: (Set<String>) -> Unit,
  showInactivityPicker: Boolean,
  onDismissInactivityPicker: () -> Unit,
  onInactivitySaved: (Set<String>) -> Unit,
  activeConfigType: RuleType?,
  onDismissConfigureServiceState: () -> Unit,
  onConfigureServiceStateConfirmed: (TriggerRule) -> Unit,
  onDismissConfigureKillOldestApp: () -> Unit,
  onConfigureKillOldestAppConfirmed: (TriggerRule) -> Unit,
) {
  if (showRuleSelection) {
    RuleSelectionDialog(
      onDismiss = onDismissRuleSelection,
      onSelectRuleType = onSelectRuleType,
      excludeRuleTypes = setOf(RuleType.KILL_OLDEST_APP, RuleType.APP_BACKGROUND_STARTED),
    )
  }

  if (showAppOpenedPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_opened),
      initialSelectedPackages = emptySet(),
      loadAllApps = { callback -> context.loadAllApps(callback) },
      onDismiss = onDismissAppOpenedPicker,
      onSave = onAppOpenedSaved,
    )
  }

  if (showAppResumedPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_resumed),
      initialSelectedPackages = emptySet(),
      loadAllApps = { callback -> context.loadAllApps(callback) },
      onDismiss = onDismissAppResumedPicker,
      onSave = onAppResumedSaved,
    )
  }

  if (showAppClosedPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_closed),
      initialSelectedPackages = emptySet(),
      loadAllApps = { callback -> context.loadAllApps(callback) },
      onDismiss = onDismissAppClosedPicker,
      onSave = onAppClosedSaved,
    )
  }

  if (showAppKilledPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_killed_manually),
      initialSelectedPackages = emptySet(),
      loadAllApps = { callback -> context.loadAllApps(callback) },
      onDismiss = onDismissAppKilledPicker,
      onSave = onAppKilledSaved,
    )
  }

  if (showAppRamPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_ram_exceeded),
      initialSelectedPackages = emptySet(),
      loadAllApps = { callback -> context.loadAllApps(callback) },
      onDismiss = onDismissAppRamPicker,
      onSave = onAppRamSaved,
    )
  }

  if (showInactivityPicker) {
    AppSelectionDialog(
      title = stringResource(R.string.rule_app_inactivity),
      initialSelectedPackages = emptySet(),
      loadAllApps = { callback -> context.loadAllApps(callback) },
      onDismiss = onDismissInactivityPicker,
      onSave = onInactivitySaved,
    )
  }

  if (activeConfigType == RuleType.SERVICE_STATE_CHANGED) {
    ConfigureServiceStateDialog(
      onDismiss = onDismissConfigureServiceState,
      onConfirm = onConfigureServiceStateConfirmed,
    )
  }

  if (activeConfigType == RuleType.KILL_OLDEST_APP) {
    ConfigureKillOldestAppDialog(
      onDismiss = onDismissConfigureKillOldestApp,
      onConfirm = onConfigureKillOldestAppConfirmed,
    )
  }
}

@Composable
fun ServiceCustomizationSelectAppsDialogs(
  context: Context,
  showExcludeDialog: Boolean,
  onDismissExcludeDialog: () -> Unit,
  onExcludedAppsSaved: (Set<String>) -> Unit,
  excludedApps: Set<String>,
  showManualDialog: Boolean,
  onDismissManualDialog: () -> Unit,
  onManuallySelectedAppsSaved: (Set<String>) -> Unit,
  manuallySelectedApps: Set<String>,
) {
  if (showExcludeDialog) {
    AppSelectionDialog(
      title = stringResource(R.string.exclude_apps),
      initialSelectedPackages = excludedApps,
      loadAllApps = { callback -> context.loadAllApps(callback) },
      onDismiss = onDismissExcludeDialog,
      onSave = onExcludedAppsSaved,
    )
  }

  if (showManualDialog) {
    AppSelectionDialog(
      title = stringResource(R.string.manually_select_apps),
      initialSelectedPackages = manuallySelectedApps,
      loadAllApps = { callback -> context.loadAllApps(callback) },
      onDismiss = onDismissManualDialog,
      onSave = onManuallySelectedAppsSaved,
    )
  }
}
