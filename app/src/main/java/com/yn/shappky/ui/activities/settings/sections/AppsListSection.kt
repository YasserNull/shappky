package com.yn.shappky.ui.activities.settings

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
import com.yn.shappky.R
import com.yn.shappky.ui.components.ActionSettingRow
import com.yn.shappky.ui.components.SettingsHeader
import com.yn.shappky.ui.components.SwitchSettingRow
import com.yn.shappky.ui.dialogs.FilterDialog
import com.yn.shappky.ui.dialogs.ProtectedAppsDialog
import com.yn.shappky.ui.dialogs.RefreshIntervalDialog
import com.yn.shappky.ui.dialogs.SortDialog
import com.yn.shappky.ui.dialogs.formatInterval
import com.yn.shappky.utils.ProtectionManager
import com.yn.shappky.utils.loadAllApps

@Composable
fun AppsListSection() {
  val context = LocalContext.current
  val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

  var showUserApps by remember { mutableStateOf(sharedPreferences.getBoolean("showUserApps", true)) }
  var showSystemApps by remember { mutableStateOf(sharedPreferences.getBoolean("showSystemApps", false)) }
  var showPersistentApps by remember { mutableStateOf(sharedPreferences.getBoolean("showPersistentApps", false)) }
  var showProtectedApps by remember { mutableStateOf(sharedPreferences.getBoolean("showProtectedApps", true)) }
  var showAppTypeIcons by remember { mutableStateOf(sharedPreferences.getBoolean("showAppTypeIcons", true)) }

  var appsAutoRefresh by remember { mutableStateOf(sharedPreferences.getBoolean("appsAutoRefresh", false)) }
  var appsRamUsageAutoRefresh by remember { mutableStateOf(sharedPreferences.getBoolean("appsRamUsageAutoRefresh", false)) }
  var appsAutoRefreshIntervalMs by remember {
    mutableStateOf(sharedPreferences.getLong("appsAutoRefreshIntervalMs", 5000L).coerceAtLeast(1000L))
  }
  var appsRamUsageRefreshIntervalMs by remember {
    mutableStateOf(sharedPreferences.getLong("appsRamUsageRefreshIntervalMs", 3000L).coerceAtLeast(1000L))
  }

  var sortMode by remember { mutableStateOf(sharedPreferences.getString("sortMode", "name") ?: "name") }
  var sortDescending by remember { mutableStateOf(sharedPreferences.getBoolean("sortDescending", false)) }
  var hiddenApps by remember { mutableStateOf(sharedPreferences.getStringSet("hidden_apps", emptySet()) ?: emptySet()) }

  var showSortDialog by remember { mutableStateOf(false) }
  var showFilterDialog by remember { mutableStateOf(false) }
  var showProtectedAppsListDialog by remember { mutableStateOf(false) }
  var showAppsAutoRefreshIntervalDialog by remember { mutableStateOf(false) }
  var showAppsRamUsageRefreshIntervalDialog by remember { mutableStateOf(false) }

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
  SwitchSettingRow(
    icon = Icons.Filled.Refresh,
    title = stringResource(R.string.apps_ram_usage_auto_refresh),
    summary = formatInterval(appsRamUsageRefreshIntervalMs),
    checked = appsRamUsageAutoRefresh,
    onCheckedChange = {
      appsRamUsageAutoRefresh = it
      sharedPreferences.edit().putBoolean("appsRamUsageAutoRefresh", it).apply()
    },
    onClick = { showAppsRamUsageRefreshIntervalDialog = true },
  )
  ActionSettingRow(
    icon = Icons.Filled.Security,
    title = stringResource(R.string.protected_apps_list_title),
    summary = stringResource(R.string.protected_apps_list_summary),
    onClick = { showProtectedAppsListDialog = true },
  )

  if (showSortDialog) {
    SortDialog(
      initialSortMode = sortMode,
      initialDescending = sortDescending,
      sortByName = "name",
      sortByRam = "ram",
      onDismiss = { showSortDialog = false },
      onApply = { newMode, descending ->
        sortMode = newMode
        sortDescending = descending
        sharedPreferences.edit()
          .putString("sortMode", newMode)
          .putBoolean("sortDescending", descending)
          .apply()
        showSortDialog = false
      },
    )
  }
  if (showFilterDialog) {
    FilterDialog(
      hiddenApps = hiddenApps,
      loadAllApps = { onLoaded -> context.loadAllApps(onLoaded) },
      onSaveHiddenApps = { newHiddenApps ->
        hiddenApps = newHiddenApps
        sharedPreferences.edit().putStringSet("hidden_apps", newHiddenApps).apply()
      },
      onDismiss = { showFilterDialog = false },
      onSaved = { showFilterDialog = false },
    )
  }
  if (showProtectedAppsListDialog) {
    ProtectedAppsDialog(
      loadAllApps = { onLoaded -> context.loadAllApps(onLoaded) },
      onDismiss = { showProtectedAppsListDialog = false },
      onSave = { apps ->
        ProtectionManager.saveProtectedApps(context, apps)
        showProtectedAppsListDialog = false
      },
    )
  }
  if (showAppsAutoRefreshIntervalDialog) {
    RefreshIntervalDialog(
      title = stringResource(R.string.apps_auto_refresh_interval_title),
      currentIntervalMs = appsAutoRefreshIntervalMs,
      choices = listOf(1000L, 2000L, 5000L, 10000L, 30000L, 60000L, 120000L, 300000L),
      onApply = { newInterval ->
        val coerced = newInterval.coerceAtLeast(1000L)
        appsAutoRefreshIntervalMs = coerced
        sharedPreferences.edit().putLong("appsAutoRefreshIntervalMs", coerced).apply()
        if (!appsAutoRefresh) {
          appsAutoRefresh = true
          sharedPreferences.edit().putBoolean("appsAutoRefresh", true).apply()
        }
        showAppsAutoRefreshIntervalDialog = false
      },
      onDismiss = { showAppsAutoRefreshIntervalDialog = false },
    )
  }
  if (showAppsRamUsageRefreshIntervalDialog) {
    RefreshIntervalDialog(
      title = stringResource(R.string.apps_ram_usage_auto_refresh_interval_title),
      currentIntervalMs = appsRamUsageRefreshIntervalMs,
      choices = listOf(1000L, 2000L, 3000L, 5000L, 10000L, 30000L, 60000L),
      onApply = { newInterval ->
        val coerced = newInterval.coerceAtLeast(1000L)
        appsRamUsageRefreshIntervalMs = coerced
        sharedPreferences.edit().putLong("appsRamUsageRefreshIntervalMs", coerced).apply()
        if (!appsRamUsageAutoRefresh) {
          appsRamUsageAutoRefresh = true
          sharedPreferences.edit().putBoolean("appsRamUsageAutoRefresh", true).apply()
        }
        showAppsRamUsageRefreshIntervalDialog = false
      },
      onDismiss = { showAppsRamUsageRefreshIntervalDialog = false },
    )
  }
}
