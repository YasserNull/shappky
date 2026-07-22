package com.yn.shappky.ui.activities.main.events

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yn.shappky.App
import com.yn.shappky.core.preferences.AppsListPreferences
import com.yn.shappky.core.preferences.KEY_DYNAMIC_COLORS
import com.yn.shappky.core.preferences.KEY_LANGUAGE
import com.yn.shappky.core.preferences.KEY_THEME
import com.yn.shappky.core.preferences.PREFERENCES_NAME
import com.yn.shappky.core.preferences.RamUsageBarPreferences
import com.yn.shappky.core.preferences.ShappkyTheme
import com.yn.shappky.core.preferences.applyDynamicColorsFromPreferences
import com.yn.shappky.core.preferences.applySystemBars
import com.yn.shappky.core.preferences.applyThemeFromPreferences
import com.yn.shappky.data.models.AppDetailedInfo
import com.yn.shappky.ui.activities.main.MainActions
import com.yn.shappky.ui.activities.main.MainActivity
import com.yn.shappky.ui.activities.main.MainContent
import com.yn.shappky.ui.activities.main.PermissionHandler
import com.yn.shappky.ui.activities.main.logic.AppsListLogic
import com.yn.shappky.utils.AutoRefreshManager
import com.yn.shappky.utils.BackgroundAppManager
import com.yn.shappky.utils.LanguageHelper
import com.yn.shappky.utils.PermissionUtils
import com.yn.shappky.utils.RamMonitor
import com.yn.shappky.utils.ShellManager
import com.yn.shappky.utils.TriggerServiceManager
import com.yn.shappky.utils.applyPendingFullScreenPreference

fun MainActivity.handleOnCreate(savedInstanceState: Bundle?) {
  val activity = this
  applyPendingFullScreenPreference()
  applyThemeFromPreferences()
  applyDynamicColorsFromPreferences()

  val prefs = getSharedPreferences(PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
  AppsListLogic.currentTheme = prefs.getString(KEY_THEME, "dark") ?: "dark"
  AppsListLogic.currentDynamicColors = prefs.getBoolean(KEY_DYNAMIC_COLORS, false)
  AppsListLogic.currentLanguage = prefs.getString(KEY_LANGUAGE, "system") ?: "system"

  val needsNotificationPermission = PermissionHandler.checkAndRequestNotificationPermission(this)

  AppsListLogic.shellManager = ShellManager(this.applicationContext, AppsListLogic.handler, AppsListLogic.executor)
  AppsListLogic.appManager = BackgroundAppManager(this.applicationContext, AppsListLogic.handler, AppsListLogic.executor, AppsListLogic.shellManager)
  AppsListLogic.autoRefreshManager = AutoRefreshManager(AppsListLogic.handler)

  AppsListLogic.ramMonitor = RamMonitor(AppsListLogic.handler) { AppsListLogic.ramState = it }

  AppsListLogic.showUserApps = prefs.getBoolean(AppsListPreferences.KEY_SHOW_USER_APPS, true)
  AppsListLogic.showSystemApps = prefs.getBoolean(AppsListPreferences.KEY_SHOW_SYSTEM_APPS, false)
  AppsListLogic.showPersistentApps = prefs.getBoolean(AppsListPreferences.KEY_SHOW_PERSISTENT_APPS, false)
  AppsListLogic.showProtectedApps = prefs.getBoolean(AppsListPreferences.KEY_SHOW_PROTECTED_APPS, true)
  AppsListLogic.showAppTypeIcons = prefs.getBoolean(AppsListPreferences.KEY_SHOW_APP_TYPE_ICONS, true)
  AppsListLogic.appsAutoRefresh = prefs.getBoolean(AppsListPreferences.KEY_APPS_AUTO_REFRESH, false)

  val ramUsageBarRefreshIntervalMs = prefs.getLong(
    RamUsageBarPreferences.KEY_REFRESH_INTERVAL_MS,
    RamUsageBarPreferences.DEFAULT_REFRESH_INTERVAL_MS,
  ).coerceAtLeast(500L)

  AppsListLogic.ramMonitor.setRefreshIntervalMs(ramUsageBarRefreshIntervalMs)

  AppsListLogic.appManager.setShowUserApps(AppsListLogic.showUserApps)
  AppsListLogic.appManager.setShowSystemApps(AppsListLogic.showSystemApps)
  AppsListLogic.appManager.setShowPersistentApps(AppsListLogic.showPersistentApps)
  AppsListLogic.appManager.setShowProtectedApps(AppsListLogic.showProtectedApps)

  PermissionHandler.setupShizukuPermissionListener(this, AppsListLogic.shellManager)
  AppsListLogic.shellManager.setOnShizukuServiceConnected(
    Runnable {
      AppsListLogic.shellManager.deployToybox(App.nativeLibraryDir)
      if (AppsListLogic.appsDataList.isEmpty()) {
        AppsListLogic.loadBackgroundApps(
          activity = activity,
          showRefreshIndicator = true,
          appsAutoRefresh = AppsListLogic.appsAutoRefresh,
          onMenuVisibilityUpdated = { AppsListLogic.forceMenuVisibilityUpdate(activity) },
        )
      }
    },
  )

  if (!needsNotificationPermission) {
    val mode = prefs.getString("permission_mode", "auto") ?: "auto"
    PermissionUtils.checkAndRequestShizukuFlow(this, mode, AppsListLogic.shellManager)
  }

  AppsListLogic.updatePermissionUi(activity, appsAutoRefresh = AppsListLogic.appsAutoRefresh)
  AppsListLogic.ramMonitor.startMonitoring()

  AppsListLogic.setupAutoRefresh(this)

  TriggerServiceManager.updateTriggerServiceState(this)
  applySystemBars()
  val appLanguage = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
  LanguageHelper.updateLauncherComponent(this, appLanguage)

  setContent {
    var selectedAppForInfo by remember { mutableStateOf<AppDetailedInfo?>(null) }
    var isFetchingInfo by remember { mutableStateOf(false) }

    ShappkyTheme {
      MainContent(
        apps = AppsListLogic.appsDataList,
        ramState = AppsListLogic.ramState,
        hasPermission = AppsListLogic.hasPermission,
        isLoadingBackgroundApps = AppsListLogic.isLoadingBackgroundApps,
        showUserApps = AppsListLogic.showUserApps,
        showSystemApps = AppsListLogic.showSystemApps,
        showPersistentApps = AppsListLogic.showPersistentApps,
        showProtectedApps = AppsListLogic.showProtectedApps,
        showAppTypeIcons = AppsListLogic.showAppTypeIcons,
        initialSortMode = prefs.getString(AppsListPreferences.KEY_SORT_MODE, AppsListPreferences.SORT_BY_NAME) ?: AppsListPreferences.SORT_BY_NAME,
        initialSortDescending = prefs.getBoolean(AppsListPreferences.KEY_SORT_DESCENDING, false),
        sortByName = AppsListPreferences.SORT_BY_NAME,
        sortByRam = AppsListPreferences.SORT_BY_RAM,
        hiddenApps = AppsListLogic.appManager.getHiddenApps(),
        onSelectAll = { selected ->
          AppsListLogic.replaceAllSelection(activity, selected)
          AppsListLogic.forceMenuVisibilityUpdate(activity)
        },
        onRefresh = {
          AppsListLogic.onRefresh(activity)
        },
        onToggleShowUserApps = {
          AppsListLogic.onToggleShowUserApps(
            AppsListLogic.showUserApps,
            AppsListLogic.showSystemApps,
            AppsListLogic.showPersistentApps,
            AppsListLogic.showProtectedApps,
            activity,
            onUpdateValue = { AppsListLogic.showUserApps = it },
          )
        },
        onToggleShowSystemApps = {
          AppsListLogic.onToggleShowSystemApps(
            AppsListLogic.showSystemApps,
            AppsListLogic.showUserApps,
            AppsListLogic.showPersistentApps,
            AppsListLogic.showProtectedApps,
            activity,
            onUpdateValue = { AppsListLogic.showSystemApps = it },
          )
        },
        onToggleShowPersistentApps = {
          AppsListLogic.onToggleShowPersistentApps(
            AppsListLogic.showPersistentApps,
            AppsListLogic.showUserApps,
            AppsListLogic.showSystemApps,
            AppsListLogic.showProtectedApps,
            activity,
            onUpdateValue = { AppsListLogic.showPersistentApps = it },
          )
        },
        onToggleShowProtectedApps = {
          AppsListLogic.onToggleShowProtectedApps(
            AppsListLogic.showProtectedApps,
            AppsListLogic.showUserApps,
            AppsListLogic.showSystemApps,
            AppsListLogic.showPersistentApps,
            activity,
            onUpdateValue = { AppsListLogic.showProtectedApps = it },
          )
        },
        onOpenSettings = {
          MainActions.onOpenSettings(activity)
        },
        onOpenDonate = {
          MainActions.onOpenDonate(activity)
        },
        onKillSelected = {
          AppsListLogic.onKillSelected(activity)
        },
        onToggleApp = { app ->
          AppsListLogic.onToggleApp(app, activity)
        },
        onKillApp = { app, force ->
          AppsListLogic.onKillApp(app, force, activity)
        },
        onApplySort = { sortMode, descending ->
          AppsListLogic.onApplySort(sortMode, descending, activity)
        },
        onLoadAllApps = { onLoaded ->
          AppsListLogic.appManager.loadAllApps { result -> onLoaded(result) }
        },
        onSaveHiddenApps = {
          AppsListLogic.appManager.saveHiddenApps(it)
        },
        onFilterSaved = {
          AppsListLogic.loadBackgroundApps(activity, true, AppsListLogic.appsAutoRefresh) { AppsListLogic.forceMenuVisibilityUpdate(activity) }
        },
        onOpenTriggers = {
          MainActions.onOpenTriggers(activity)
        },
        isServiceRunning = AppsListLogic.isServiceRunning,
        onToggleService = { start ->
          MainActions.onToggleService(activity, start, AppsListLogic.shellManager)
        },
        onAppLongClick = { app ->
          AppsListLogic.onAppLongClick(
            app,
            AppsListLogic.appManager,
            onFetchingStateChange = { fetching -> isFetchingInfo = fetching },
            onResult = { result -> selectedAppForInfo = result },
          )
        },
        selectedAppForInfo = selectedAppForInfo,
        onDismissAppInfo = { selectedAppForInfo = null },
      )
    }
  }
}
