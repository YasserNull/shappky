package com.yn.shappky.core.preferences

object AppsListPreferences {
  const val KEY_SHOW_USER_APPS = "showUserApps"
  const val KEY_SHOW_SYSTEM_APPS = "showSystemApps"
  const val KEY_SHOW_PERSISTENT_APPS = "showPersistentApps"
  const val KEY_SHOW_PROTECTED_APPS = "showProtectedApps"
  const val KEY_SHOW_APP_TYPE_ICONS = "showAppTypeIcons"

  const val KEY_SORT_MODE = "sortMode"
  const val KEY_SORT_DESCENDING = "sortDescending"

  const val SORT_BY_NAME = "name"
  const val SORT_BY_RAM = "ram"
  const val SORT_BY_TYPE = "type"

  const val KEY_APPS_AUTO_REFRESH = "appsAutoRefresh"
  const val KEY_APPS_RAM_USAGE_AUTO_REFRESH = "appsRamUsageAutoRefresh"
  const val KEY_APPS_AUTO_REFRESH_INTERVAL_MS = "appsAutoRefreshIntervalMs"
  const val KEY_APPS_RAM_USAGE_REFRESH_INTERVAL_MS = "appsRamUsageRefreshIntervalMs"

  const val DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS = 5000L
  const val DEFAULT_APPS_RAM_USAGE_REFRESH_INTERVAL_MS = 3000L
}
