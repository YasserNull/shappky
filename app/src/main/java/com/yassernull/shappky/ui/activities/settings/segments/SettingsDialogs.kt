package com.yassernull.shappky.ui.activities.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R
import com.yassernull.shappky.ui.dialogs.FilterDialog
import com.yassernull.shappky.ui.dialogs.LanguageDialog
import com.yassernull.shappky.ui.dialogs.PermissionModeDialog
import com.yassernull.shappky.ui.dialogs.ProtectedAppsDialog
import com.yassernull.shappky.ui.dialogs.RefreshIntervalDialog
import com.yassernull.shappky.ui.dialogs.RestartDialog
import com.yassernull.shappky.ui.dialogs.SortDialog
import com.yassernull.shappky.ui.dialogs.ThemeDialog

@Composable
fun LanguageSettingsDialog(
  languageValue: String,
  options: Array<String>,
  showDialog: Boolean,
  onLanguageSelected: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  if (showDialog) {
    LanguageDialog(
      languageValue = languageValue,
      options = options,
      onLanguageSelected = onLanguageSelected,
      onDismiss = onDismiss,
    )
  }
}

@Composable
fun PermissionSettingsDialog(
  permissionMode: String,
  showDialog: Boolean,
  onModeSelected: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  if (showDialog) {
    PermissionModeDialog(
      permissionMode = permissionMode,
      onModeSelected = onModeSelected,
      onDismiss = onDismiss,
    )
  }
}

@Composable
fun ThemeSettingsDialogs(
  themeValue: String,
  options: Array<String>,
  showThemeDialog: Boolean,
  showRestartDialog: Boolean,
  onThemeSelected: (String) -> Unit,
  onRestart: () -> Unit,
  onDismissTheme: () -> Unit,
  onDismissRestart: () -> Unit,
) {
  if (showThemeDialog) {
    ThemeDialog(
      themeValue = themeValue,
      options = options,
      onThemeSelected = onThemeSelected,
      onDismiss = onDismissTheme,
    )
  }
  if (showRestartDialog) {
    RestartDialog(
      onRestart = onRestart,
      onDismiss = onDismissRestart,
    )
  }
}

@Composable
fun RamUsageSettingsDialog(
  ramUsageBarRefreshIntervalMs: Long,
  showDialog: Boolean,
  onApply: (Long) -> Unit,
  onDismiss: () -> Unit,
) {
  if (showDialog) {
    RefreshIntervalDialog(
      title = stringResource(R.string.ram_usage_bar_refresh_interval_title),
      currentIntervalMs = ramUsageBarRefreshIntervalMs,
      choices = listOf(500L, 1000L, 2000L, 5000L),
      onApply = { newInterval -> onApply(newInterval.coerceAtLeast(500L)) },
      onDismiss = onDismiss,
    )
  }
}

@Composable
fun AppsListSettingsDialogs(
  sortMode: String,
  sortDescending: Boolean,
  hiddenApps: Set<String>,
  appsAutoRefreshIntervalMs: Long,
  showSortDialog: Boolean,
  showFilterDialog: Boolean,
  showProtectedAppsListDialog: Boolean,
  showAppsAutoRefreshIntervalDialog: Boolean,
  onSortApply: (String, Boolean) -> Unit,
  onSaveHiddenApps: (Set<String>) -> Unit,
  onSaveProtectedApps: (Set<String>) -> Unit,
  onAutoRefreshApply: (Long) -> Unit,
  onDismissSort: () -> Unit,
  onDismissFilter: () -> Unit,
  onDismissProtectedApps: () -> Unit,
  onDismissAutoRefresh: () -> Unit,
  loadAllApps: ((List<com.yassernull.shappky.data.models.AppModel>) -> Unit) -> Unit,
  saveProtectedApps: (Set<String>) -> Unit,
) {
  if (showSortDialog) {
    SortDialog(
      initialSortMode = sortMode,
      initialDescending = sortDescending,
      sortByName = "name",
      sortByRam = "ram",
      sortByCpu = "cpu",
      onDismiss = onDismissSort,
      onApply = onSortApply,
    )
  }
  if (showFilterDialog) {
    FilterDialog(
      hiddenApps = hiddenApps,
      loadAllApps = loadAllApps,
      onSaveHiddenApps = onSaveHiddenApps,
      onDismiss = onDismissFilter,
      onSaved = onDismissFilter,
    )
  }
  if (showProtectedAppsListDialog) {
    ProtectedAppsDialog(
      loadAllApps = loadAllApps,
      onDismiss = onDismissProtectedApps,
      onSave = onSaveProtectedApps,
    )
  }
  if (showAppsAutoRefreshIntervalDialog) {
    RefreshIntervalDialog(
      title = stringResource(R.string.apps_auto_refresh_interval_title),
      currentIntervalMs = appsAutoRefreshIntervalMs,
      choices = listOf(1000L, 2000L, 5000L, 10000L, 30000L, 60000L, 120000L, 300000L),
      onApply = { newInterval -> onAutoRefreshApply(newInterval.coerceAtLeast(1000L)) },
      onDismiss = onDismissAutoRefresh,
    )
  }
}
