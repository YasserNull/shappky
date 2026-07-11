package com.yn.shappky.ui.activities.listWidgetConfig

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R
import com.yn.shappky.ui.components.ColorPickerDialog
import com.yn.shappky.ui.dialogs.RefreshIntervalDialog
import com.yn.shappky.ui.dialogs.SortDialog

@Composable
fun ListWidgetConfigAppsListDialogs(
  showSortDialog: Boolean,
  onDismissSortDialog: () -> Unit,
  onApplySort: (String, Boolean) -> Unit,
  sortMode: String,
  sortDescending: Boolean,
  showAppsAutoRefreshIntervalDialog: Boolean,
  onDismissAppsAutoRefreshIntervalDialog: () -> Unit,
  onApplyAppsAutoRefreshInterval: (Long) -> Unit,
  appsAutoRefreshIntervalMs: Long,
  showAppsRamUsageRefreshIntervalDialog: Boolean,
  onDismissAppsRamUsageRefreshIntervalDialog: () -> Unit,
  onApplyAppsRamUsageRefreshInterval: (Long) -> Unit,
  appsRamUsageRefreshIntervalMs: Long,
) {
  if (showSortDialog) {
    SortDialog(
      initialSortMode = sortMode,
      initialDescending = sortDescending,
      sortByName = "name",
      sortByRam = "ram",
      onDismiss = onDismissSortDialog,
      onApply = onApplySort,
    )
  }

  if (showAppsAutoRefreshIntervalDialog) {
    RefreshIntervalDialog(
      title = stringResource(R.string.apps_auto_refresh_interval_title),
      currentIntervalMs = appsAutoRefreshIntervalMs,
      choices = listOf(1000L, 2000L, 5000L, 10000L, 30000L, 60000L, 120000L, 300000L),
      onApply = onApplyAppsAutoRefreshInterval,
      onDismiss = onDismissAppsAutoRefreshIntervalDialog,
    )
  }

  if (showAppsRamUsageRefreshIntervalDialog) {
    RefreshIntervalDialog(
      title = stringResource(R.string.apps_ram_usage_auto_refresh_interval_title),
      currentIntervalMs = appsRamUsageRefreshIntervalMs,
      choices = listOf(1000L, 2000L, 3000L, 5000L, 10000L, 30000L, 60000L),
      onApply = onApplyAppsRamUsageRefreshInterval,
      onDismiss = onDismissAppsRamUsageRefreshIntervalDialog,
    )
  }
}

@Composable
fun ListWidgetConfigRamUsageBarDialogs(
  showRamUsageBarRefreshIntervalDialog: Boolean,
  onDismissRamUsageBarRefreshIntervalDialog: () -> Unit,
  onApplyRamUsageBarRefreshInterval: (Long) -> Unit,
  ramUsageBarRefreshIntervalMs: Long,
) {
  if (showRamUsageBarRefreshIntervalDialog) {
    RefreshIntervalDialog(
      title = stringResource(R.string.ram_usage_bar_refresh_interval_title),
      currentIntervalMs = ramUsageBarRefreshIntervalMs,
      choices = listOf(500L, 1000L, 2000L, 5000L),
      onApply = onApplyRamUsageBarRefreshInterval,
      onDismiss = onDismissRamUsageBarRefreshIntervalDialog,
    )
  }
}

@Composable
fun ListWidgetConfigThemeDialogs(
  showColorPicker: Boolean,
  onDismissColorPicker: () -> Unit,
  onColorSelected: (Int) -> Unit,
  bgColor: Int,
) {
  if (showColorPicker) {
    ColorPickerDialog(
      initialColor = bgColor,
      onDismiss = onDismissColorPicker,
      onColorSelected = onColorSelected,
    )
  }
}
