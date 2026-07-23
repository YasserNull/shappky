package com.yn.shappky.ui.activities.main.events

import com.yn.shappky.core.preferences.AppsListPreferences
import com.yn.shappky.core.preferences.KEY_DYNAMIC_COLORS
import com.yn.shappky.core.preferences.KEY_LANGUAGE
import com.yn.shappky.core.preferences.KEY_THEME
import com.yn.shappky.core.preferences.PREFERENCES_NAME
import com.yn.shappky.core.preferences.RamUsageBarPreferences
import com.yn.shappky.core.preferences.applySystemBars
import com.yn.shappky.services.ShappkyService
import com.yn.shappky.ui.activities.main.MainActivity
import com.yn.shappky.ui.activities.main.logic.AppsListLogic
import com.yn.shappky.ui.activities.main.logic.AppsRamUsageLogic

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
  val showAppTypeIconsNow = prefs.getBoolean(AppsListPreferences.KEY_SHOW_APP_TYPE_ICONS, true)

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

  if (showAppTypeIconsNow != AppsListLogic.showAppTypeIcons) {
    AppsListLogic.showAppTypeIcons = showAppTypeIconsNow
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
