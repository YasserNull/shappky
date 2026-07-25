package com.yassernull.shappky.ui.activities.listWidgetConfig.sections

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R
import com.yassernull.shappky.core.preferences.AppsListPreferences
import com.yassernull.shappky.core.preferences.WidgetPreferences
import com.yassernull.shappky.ui.activities.listWidgetConfig.ListWidgetConfigAppsListDialogs
import com.yassernull.shappky.ui.components.ActionSettingRow
import com.yassernull.shappky.ui.components.RowSetting
import com.yassernull.shappky.ui.components.SectionHeader

@Composable
fun ListWidgetConfigAppsList(appWidgetId: Int) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }

  var showUserApps by remember {
    mutableStateOf(prefs.getBoolean(WidgetPreferences.getListShowUserAppsKey(appWidgetId), true))
  }
  var showSystemApps by remember {
    mutableStateOf(prefs.getBoolean(WidgetPreferences.getListShowSystemAppsKey(appWidgetId), true))
  }
  var showPersistentApps by remember {
    mutableStateOf(prefs.getBoolean(WidgetPreferences.getListShowPersistentAppsKey(appWidgetId), false))
  }
  var showProtectedApps by remember {
    mutableStateOf(prefs.getBoolean(WidgetPreferences.getListShowProtectedAppsKey(appWidgetId), false))
  }
  var showAppTypeIcons by remember {
    mutableStateOf(prefs.getBoolean(WidgetPreferences.getListShowAppTypeIconsKey(appWidgetId), true))
  }
  var sortMode by remember {
    mutableStateOf(prefs.getString(WidgetPreferences.getListSortModeKey(appWidgetId), AppsListPreferences.SORT_BY_NAME) ?: AppsListPreferences.SORT_BY_NAME)
  }
  var sortDescending by remember {
    mutableStateOf(prefs.getBoolean(WidgetPreferences.getListSortDescendingKey(appWidgetId), false))
  }
  var autoRefreshApps by remember {
    mutableStateOf(prefs.getBoolean(WidgetPreferences.getListAutoRefreshAppsKey(appWidgetId), true))
  }
  var autoRefreshRam by remember {
    mutableStateOf(prefs.getBoolean(WidgetPreferences.getListAutoRefreshRamKey(appWidgetId), true))
  }
  var appsAutoRefreshIntervalMs by remember {
    mutableStateOf(prefs.getLong("appsAutoRefreshIntervalMs", 1000L))
  }
  var appsRamUsageRefreshIntervalMs by remember {
    mutableStateOf(prefs.getLong("appsRamUsageRefreshIntervalMs", 1000L))
  }

  var showSortDialog by remember { mutableStateOf(false) }
  var showAppsAutoRefreshIntervalDialog by remember { mutableStateOf(false) }
  var showAppsRamUsageRefreshIntervalDialog by remember { mutableStateOf(false) }

  SectionHeader(stringResource(R.string.widget_list_apps_title))
  RowSetting(stringResource(R.string.show_user_apps), showUserApps) {
    showUserApps = it
    prefs.edit().putBoolean(WidgetPreferences.getListShowUserAppsKey(appWidgetId), it).apply()
  }
  RowSetting(stringResource(R.string.show_system_apps), showSystemApps) {
    showSystemApps = it
    prefs.edit().putBoolean(WidgetPreferences.getListShowSystemAppsKey(appWidgetId), it).apply()
  }
  RowSetting(stringResource(R.string.show_persistent_apps), showPersistentApps) {
    showPersistentApps = it
    prefs.edit().putBoolean(WidgetPreferences.getListShowPersistentAppsKey(appWidgetId), it).apply()
  }
  RowSetting(stringResource(R.string.show_protected_apps), showProtectedApps) {
    showProtectedApps = it
    prefs.edit().putBoolean(WidgetPreferences.getListShowProtectedAppsKey(appWidgetId), it).apply()
  }
  RowSetting(stringResource(R.string.show_app_type_icons), showAppTypeIcons) {
    showAppTypeIcons = it
    prefs.edit().putBoolean(WidgetPreferences.getListShowAppTypeIconsKey(appWidgetId), it).apply()
  }

  val sortModeText = when (sortMode) {
    "ram" -> stringResource(R.string.sort_by_ram_usage)
    "type" -> stringResource(R.string.sort_by_type)
    else -> stringResource(R.string.sort_by_name)
  }
  val sortSummary = if (sortDescending) {
    "$sortModeText (${stringResource(R.string.sort_descending)})"
  } else {
    sortModeText
  }

  ActionSettingRow(
    label = stringResource(R.string.sort_apps),
    summary = sortSummary,
    onClick = { showSortDialog = true },
  )

  RowSetting(stringResource(R.string.apps_auto_refresh), autoRefreshApps) {
    autoRefreshApps = it
    prefs.edit().putBoolean(WidgetPreferences.getListAutoRefreshAppsKey(appWidgetId), it).apply()
  }

  RowSetting(stringResource(R.string.apps_ram_usage_auto_refresh), autoRefreshRam) {
    autoRefreshRam = it
    prefs.edit().putBoolean(WidgetPreferences.getListAutoRefreshRamKey(appWidgetId), it).apply()
  }

  ListWidgetConfigAppsListDialogs(
    showSortDialog = showSortDialog,
    onDismissSortDialog = { showSortDialog = false },
    onApplySort = { mode, descending ->
      sortMode = mode
      sortDescending = descending
      prefs.edit()
        .putString(WidgetPreferences.getListSortModeKey(appWidgetId), mode)
        .putBoolean(WidgetPreferences.getListSortDescendingKey(appWidgetId), descending)
        .apply()
      showSortDialog = false
    },
    sortMode = sortMode,
    sortDescending = sortDescending,
    showAppsAutoRefreshIntervalDialog = showAppsAutoRefreshIntervalDialog,
    onDismissAppsAutoRefreshIntervalDialog = { showAppsAutoRefreshIntervalDialog = false },
    onApplyAppsAutoRefreshInterval = {
      appsAutoRefreshIntervalMs = it
      prefs.edit().putLong("appsAutoRefreshIntervalMs", it).apply()
      showAppsAutoRefreshIntervalDialog = false
    },
    appsAutoRefreshIntervalMs = appsAutoRefreshIntervalMs,
    showAppsRamUsageRefreshIntervalDialog = showAppsRamUsageRefreshIntervalDialog,
    onDismissAppsRamUsageRefreshIntervalDialog = { showAppsRamUsageRefreshIntervalDialog = false },
    onApplyAppsRamUsageRefreshInterval = {
      appsRamUsageRefreshIntervalMs = it
      prefs.edit().putLong("appsRamUsageRefreshIntervalMs", it).apply()
      showAppsRamUsageRefreshIntervalDialog = false
    },
    appsRamUsageRefreshIntervalMs = appsRamUsageRefreshIntervalMs,
  )
}
