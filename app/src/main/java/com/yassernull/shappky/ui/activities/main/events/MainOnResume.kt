package com.yassernull.shappky.ui.activities.main.events

import com.yassernull.shappky.core.preferences.AppsListPreferences
import com.yassernull.shappky.core.preferences.KEY_DYNAMIC_COLORS
import com.yassernull.shappky.core.preferences.KEY_LANGUAGE
import com.yassernull.shappky.core.preferences.KEY_THEME
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.core.preferences.RamUsageBarPreferences
import com.yassernull.shappky.services.ShappkyService
import com.yassernull.shappky.ui.activities.main.MainActivity
import com.yassernull.shappky.ui.activities.main.logic.AppsListLogic
import com.yassernull.shappky.ui.activities.main.logic.AppsRamUsageLogic
import com.yassernull.shappky.ui.components.applySystemBars

fun MainActivity.handleOnResume() {
  ShappkyService.registerListener(serviceStateListener)
  val prefs = getSharedPreferences(PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
  val themeNow = prefs.getString(KEY_THEME, "dark")
  val dynamicNow = prefs.getBoolean(KEY_DYNAMIC_COLORS, false)
  val languageNow = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
  if (themeNow != AppsListLogic.currentTheme || dynamicNow != AppsListLogic.currentDynamicColors || languageNow != AppsListLogic.currentLanguage) {
    recreate()
    return
  }
  applySystemBars()

  val showUserAppsNow = prefs.getBoolean(AppsListPreferences.KEY_SHOW_USER_APPS, true)
  val showSystemAppsNow = prefs.getBoolean(AppsListPreferences.KEY_SHOW_SYSTEM_APPS, true)
  val showPersistentAppsNow = prefs.getBoolean(AppsListPreferences.KEY_SHOW_PERSISTENT_APPS, false)
  val showProtectedAppsNow = prefs.getBoolean(AppsListPreferences.KEY_SHOW_PROTECTED_APPS, false)

  var settingsChanged = false
  if (showUserAppsNow != AppsListLogic.showUserApps || showSystemAppsNow != AppsListLogic.showSystemApps || showPersistentAppsNow != AppsListLogic.showPersistentApps || showProtectedAppsNow != AppsListLogic.showProtectedApps) {
    AppsListLogic.showUserApps = showUserAppsNow
    AppsListLogic.showSystemApps = showSystemAppsNow
    AppsListLogic.showPersistentApps = showPersistentAppsNow
    AppsListLogic.showProtectedApps = showProtectedAppsNow
    AppsListLogic.appManager.setShowUserApps(AppsListLogic.showUserApps)
    AppsListLogic.appManager.setShowSystemApps(AppsListLogic.showSystemApps)
    AppsListLogic.appManager.setShowPersistentApps(AppsListLogic.showPersistentApps)
    AppsListLogic.appManager.setShowProtectedApps(AppsListLogic.showProtectedApps)
    settingsChanged = true
  }

  val ramUsageBarRefreshIntervalMs = prefs.getLong(
    RamUsageBarPreferences.KEY_REFRESH_INTERVAL_MS,
    RamUsageBarPreferences.DEFAULT_REFRESH_INTERVAL_MS,
  ).coerceAtLeast(500L)

  AppsListLogic.ramMonitor.setRefreshIntervalMs(ramUsageBarRefreshIntervalMs)
  AppsListLogic.ramMonitor.startMonitoring()

  AppsListLogic.setupAutoRefresh(this)

  AppsListLogic.updatePermissionUi(this, forceRefresh = settingsChanged, appsAutoRefresh = AppsListLogic.appsAutoRefresh)
  AppsRamUsageLogic.refreshAppsRamUsage(this)

  applySystemBars()
}
