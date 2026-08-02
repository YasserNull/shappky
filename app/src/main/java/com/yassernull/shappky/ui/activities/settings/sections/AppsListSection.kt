package com.yassernull.shappky.ui.activities.settings

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.ProtectionManager
import com.yassernull.shappky.ui.components.ActionSettingRow
import com.yassernull.shappky.ui.components.SettingsHeader
import com.yassernull.shappky.ui.components.SwitchSettingRow
import com.yassernull.shappky.utils.formatInterval
import com.yassernull.shappky.utils.loadAllApps

@Composable
fun AppsListSection() {
  val context = LocalContext.current
  val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

  var showUserApps by remember { mutableStateOf(sharedPreferences.getBoolean("showUserApps", true)) }
  var showSystemApps by remember { mutableStateOf(sharedPreferences.getBoolean("showSystemApps", true)) }
  var showPersistentApps by remember { mutableStateOf(sharedPreferences.getBoolean("showPersistentApps", false)) }
  var showProtectedApps by remember { mutableStateOf(sharedPreferences.getBoolean("showProtectedApps", false)) }
  var showAppTypeIcons by remember { mutableStateOf(sharedPreferences.getBoolean("showAppTypeIcons", true)) }

  var appsAutoRefresh by remember { mutableStateOf(sharedPreferences.getBoolean("appsAutoRefresh", true)) }
  var appsAutoRefreshIntervalMs by remember {
    mutableStateOf(sharedPreferences.getLong("appsAutoRefreshIntervalMs", 1000L).coerceAtLeast(1000L))
  }

  var sortMode by remember { mutableStateOf(sharedPreferences.getString("sortMode", "name") ?: "name") }
  var sortDescending by remember { mutableStateOf(sharedPreferences.getBoolean("sortDescending", false)) }
  var hiddenApps by remember { mutableStateOf(sharedPreferences.getStringSet("hidden_apps", emptySet()) ?: emptySet()) }

  var showSortDialog by remember { mutableStateOf(false) }
  var showFilterDialog by remember { mutableStateOf(false) }
  var showProtectedAppsListDialog by remember { mutableStateOf(false) }
  var showAppsAutoRefreshIntervalDialog by remember { mutableStateOf(false) }

  SettingsHeader(text = stringResource(R.string.settings_apps_list))
  SwitchSettingRow(
    icon = Icons.Filled.Apps,
    title = stringResource(R.string.show_user_apps),
    summary = stringResource(R.string.show_user_apps_summary),
    checked = showUserApps,
    onCheckedChange = {
      if (it || showSystemApps || showPersistentApps || showProtectedApps) {
        showUserApps = it
        sharedPreferences.edit().putBoolean("showUserApps", it).apply()
      }
    },
  )
  SwitchSettingRow(
    icon = Icons.Filled.Settings,
    title = stringResource(R.string.show_system_apps),
    summary = stringResource(R.string.show_system_apps_summary),
    checked = showSystemApps,
    onCheckedChange = {
      if (it || showUserApps || showPersistentApps || showProtectedApps) {
        showSystemApps = it
        sharedPreferences.edit().putBoolean("showSystemApps", it).apply()
      }
    },
  )
  SwitchSettingRow(
    icon = Icons.Filled.Security,
    title = stringResource(R.string.show_persistent_apps),
    summary = stringResource(R.string.show_persistent_apps_summary),
    checked = showPersistentApps,
    onCheckedChange = {
      if (it || showUserApps || showSystemApps || showProtectedApps) {
        showPersistentApps = it
        sharedPreferences.edit().putBoolean("showPersistentApps", it).apply()
      }
    },
  )
  SwitchSettingRow(
    icon = Icons.Filled.DoNotDisturb,
    title = stringResource(R.string.show_protected_apps),
    summary = stringResource(R.string.show_protected_apps_summary),
    checked = showProtectedApps,
    onCheckedChange = {
      if (it || showUserApps || showSystemApps || showPersistentApps) {
        showProtectedApps = it
        sharedPreferences.edit().putBoolean("showProtectedApps", it).apply()
      }
    },
  )
  SwitchSettingRow(
    icon = Icons.Filled.Palette,
    title = stringResource(R.string.show_app_type_icons),
    summary = stringResource(R.string.show_app_type_icons_summary),
    checked = showAppTypeIcons,
    onCheckedChange = {
      showAppTypeIcons = it
      sharedPreferences.edit().putBoolean("showAppTypeIcons", it).apply()
    },
  )
  ActionSettingRow(
    icon = Icons.AutoMirrored.Filled.Sort,
    title = stringResource(R.string.sort_apps),
    summary = stringResource(R.string.sort_apps_title),
    onClick = { showSortDialog = true },
  )
  ActionSettingRow(
    icon = Icons.Filled.FilterList,
    title = stringResource(R.string.apps_filter),
    summary = stringResource(R.string.filter_dialog_title),
    onClick = { showFilterDialog = true },
  )
  SwitchSettingRow(
    icon = Icons.Filled.Refresh,
    title = stringResource(R.string.apps_auto_refresh),
    summary = formatInterval(appsAutoRefreshIntervalMs),
    checked = appsAutoRefresh,
    onCheckedChange = {
      appsAutoRefresh = it
      sharedPreferences.edit().putBoolean("appsAutoRefresh", it).apply()
    },
    onClick = { showAppsAutoRefreshIntervalDialog = true },
  )
  ActionSettingRow(
    icon = Icons.Filled.Security,
    title = stringResource(R.string.protected_apps_list_title),
    summary = stringResource(R.string.protected_apps_list_summary),
    onClick = { showProtectedAppsListDialog = true },
  )

  AppsListSettingsDialogs(
    sortMode = sortMode,
    sortDescending = sortDescending,
    hiddenApps = hiddenApps,
    appsAutoRefreshIntervalMs = appsAutoRefreshIntervalMs,
    showSortDialog = showSortDialog,
    showFilterDialog = showFilterDialog,
    showProtectedAppsListDialog = showProtectedAppsListDialog,
    showAppsAutoRefreshIntervalDialog = showAppsAutoRefreshIntervalDialog,
    onSortApply = { newMode, descending ->
      sortMode = newMode
      sortDescending = descending
      sharedPreferences.edit()
        .putString("sortMode", newMode)
        .putBoolean("sortDescending", descending)
        .apply()
      showSortDialog = false
    },
    onSaveHiddenApps = { newHiddenApps ->
      hiddenApps = newHiddenApps
      sharedPreferences.edit().putStringSet("hidden_apps", newHiddenApps).apply()
      showFilterDialog = false
    },
    onSaveProtectedApps = { apps ->
      ProtectionManager.saveProtectedApps(context, apps)
      showProtectedAppsListDialog = false
    },
    onAutoRefreshApply = { newInterval ->
      appsAutoRefreshIntervalMs = newInterval
      sharedPreferences.edit().putLong("appsAutoRefreshIntervalMs", newInterval).apply()
      if (!appsAutoRefresh) {
        appsAutoRefresh = true
        sharedPreferences.edit().putBoolean("appsAutoRefresh", true).apply()
      }
      showAppsAutoRefreshIntervalDialog = false
    },
    onDismissSort = { showSortDialog = false },
    onDismissFilter = { showFilterDialog = false },
    onDismissProtectedApps = { showProtectedAppsListDialog = false },
    onDismissAutoRefresh = { showAppsAutoRefreshIntervalDialog = false },
    loadAllApps = { onLoaded -> context.loadAllApps(onLoaded) },
    saveProtectedApps = { apps -> ProtectionManager.saveProtectedApps(context, apps) },
  )
}
