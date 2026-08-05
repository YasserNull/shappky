package com.yassernull.shappky.ui.activities.main.logic

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yassernull.shappky.core.managers.AutoRefreshManager
import com.yassernull.shappky.core.managers.BackgroundAppManager
import com.yassernull.shappky.core.managers.RamMonitorManager
import com.yassernull.shappky.core.managers.RamState
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.preferences.AppsListPreferences
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.data.models.AppModel
import com.yassernull.shappky.ui.activities.main.MainActivity
import com.yassernull.shappky.ui.components.AppRow
import com.yassernull.shappky.utils.AppSortUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsList(
  apps: List<AppModel>,
  isLoadingBackgroundApps: Boolean,
  onRefresh: () -> Unit,
  onToggleApp: (AppModel) -> Unit,
  onKillApp: (AppModel, Boolean) -> Unit,
  onAppLongClick: (AppModel) -> Unit,
) {
  PullToRefreshBox(
    isRefreshing = isLoadingBackgroundApps,
    onRefresh = onRefresh,
    modifier = Modifier.fillMaxSize(),
  ) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(apps, key = { it.packageName }) { app ->
        AppRow(
          app = app,
          onToggle = { onToggleApp(app) },
          onKill = { force -> onKillApp(app, force) },
          onLongClick = { onAppLongClick(app) },
        )
      }
    }
  }
}

object AppsListLogic {
  internal val handler = Handler(Looper.getMainLooper())
  internal val executor: ExecutorService = Executors.newSingleThreadExecutor()
  internal lateinit var shellManager: ShellManager
  internal lateinit var appManager: BackgroundAppManager
  internal lateinit var ramMonitor: RamMonitorManager

  internal val appsDataList = mutableStateListOf<AppModel>()
  internal var ramState by mutableStateOf(RamState())
  internal var isLoadingBackgroundApps by mutableStateOf(false)
  internal var hasPermission by mutableStateOf(false)
  internal var isServiceRunning by mutableStateOf(false)
  internal var backgroundLoadRetryCount = 0

  internal var currentTheme = "dark"
  internal var currentDynamicColors = false
  internal var currentLanguage = "system"

  internal var showUserApps by mutableStateOf(true)
  internal var showSystemApps by mutableStateOf(true)
  internal var showPersistentApps by mutableStateOf(false)
  internal var showProtectedApps by mutableStateOf(false)
  internal var appsAutoRefresh = true
  internal lateinit var autoRefreshManager: AutoRefreshManager
  private const val TAG = "AppsListLogic"

  fun onKillSelected(activity: MainActivity) {
    val packagesToKill = appsDataList.filter { it.isSelected }.map { it.packageName }
    if (packagesToKill.isEmpty()) return

    val isAllSelected = packagesToKill.size == appsDataList.size
    replaceAllSelection(activity, false)

    appManager.killPackages(
      packagesToKill,
      Runnable {
        loadBackgroundApps(activity, showRefreshIndicator = true, appsAutoRefresh = false) { forceMenuVisibilityUpdate(activity) }
      },
      showToast = true,
      appendKillAll = isAllSelected,
    )
  }

  fun onToggleApp(app: AppModel, activity: MainActivity) {
    if (!app.isProtected) {
      replaceApp(activity, app.copy(isSelected = !app.isSelected))
    }
  }

  fun onKillApp(
    app: AppModel,
    force: Boolean,
    activity: MainActivity,
  ) {
    appManager.killApp(
      app.packageName,
      Runnable {
        loadBackgroundApps(activity, true, appsAutoRefresh) { forceMenuVisibilityUpdate(activity) }
      },
      force,
    )
  }

  fun onApplySort(
    sortMode: String,
    descending: Boolean,
    activity: MainActivity,
  ) {
    val prefs = activity.getSharedPreferences(PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
    prefs.edit()
      .putString(AppsListPreferences.KEY_SORT_MODE, sortMode)
      .putBoolean(AppsListPreferences.KEY_SORT_DESCENDING, descending)
      .apply()
    sortAppsDataList(activity)
  }

  fun onAppLongClick(
    app: AppModel,
    appManager: BackgroundAppManager,
    onFetchingStateChange: (Boolean) -> Unit,
    onResult: (com.yassernull.shappky.data.models.AppDetailedInfo?) -> Unit,
  ) {
    onFetchingStateChange(true)
    appManager.loadAppDetailedInfo(app) { detailedInfo ->
      onResult(detailedInfo)
      onFetchingStateChange(false)
    }
  }

  fun onRefresh(
    activity: MainActivity,
  ) {
    loadBackgroundApps(
      activity = activity,
      showRefreshIndicator = true,
      appsAutoRefresh = appsAutoRefresh,
      onMenuVisibilityUpdated = { forceMenuVisibilityUpdate(activity) },
    )
  }

  fun onToggleShowUserApps(
    currentValue: Boolean,
    showSystemApps: Boolean,
    showPersistentApps: Boolean,
    showProtectedApps: Boolean,
    activity: MainActivity,
    onUpdateValue: (Boolean) -> Unit,
  ) {
    if (!currentValue || showSystemApps || showPersistentApps || showProtectedApps) {
      val newValue = !currentValue
      onUpdateValue(newValue)
      appManager.setShowUserApps(newValue)
      activity.getSharedPreferences(PREFERENCES_NAME, android.content.Context.MODE_PRIVATE).edit().putBoolean(AppsListPreferences.KEY_SHOW_USER_APPS, newValue).apply()
      replaceAllSelection(activity, false)
      loadBackgroundApps(activity, true, appsAutoRefresh) { forceMenuVisibilityUpdate(activity) }
    }
  }

  fun onToggleShowSystemApps(
    currentValue: Boolean,
    showUserApps: Boolean,
    showPersistentApps: Boolean,
    showProtectedApps: Boolean,
    activity: MainActivity,
    onUpdateValue: (Boolean) -> Unit,
  ) {
    if (!currentValue || showUserApps || showPersistentApps || showProtectedApps) {
      val newValue = !currentValue
      onUpdateValue(newValue)
      appManager.setShowSystemApps(newValue)
      activity.getSharedPreferences(PREFERENCES_NAME, android.content.Context.MODE_PRIVATE).edit().putBoolean(AppsListPreferences.KEY_SHOW_SYSTEM_APPS, newValue).apply()
      replaceAllSelection(activity, false)
      loadBackgroundApps(activity, true, appsAutoRefresh) { forceMenuVisibilityUpdate(activity) }
    }
  }

  fun onToggleShowPersistentApps(
    currentValue: Boolean,
    showUserApps: Boolean,
    showSystemApps: Boolean,
    showProtectedApps: Boolean,
    activity: MainActivity,
    onUpdateValue: (Boolean) -> Unit,
  ) {
    if (!currentValue || showUserApps || showSystemApps || showProtectedApps) {
      val newValue = !currentValue
      onUpdateValue(newValue)
      appManager.setShowPersistentApps(newValue)
      activity.getSharedPreferences(PREFERENCES_NAME, android.content.Context.MODE_PRIVATE).edit().putBoolean(AppsListPreferences.KEY_SHOW_PERSISTENT_APPS, newValue).apply()
      replaceAllSelection(activity, false)
      loadBackgroundApps(activity, true, appsAutoRefresh) { forceMenuVisibilityUpdate(activity) }
    }
  }

  fun onToggleShowProtectedApps(
    currentValue: Boolean,
    showUserApps: Boolean,
    showSystemApps: Boolean,
    showPersistentApps: Boolean,
    activity: MainActivity,
    onUpdateValue: (Boolean) -> Unit,
  ) {
    if (!currentValue || showUserApps || showSystemApps || showPersistentApps) {
      val newValue = !currentValue
      onUpdateValue(newValue)
      appManager.setShowProtectedApps(newValue)
      activity.getSharedPreferences(PREFERENCES_NAME, android.content.Context.MODE_PRIVATE).edit().putBoolean(AppsListPreferences.KEY_SHOW_PROTECTED_APPS, newValue).apply()
      replaceAllSelection(activity, false)
      loadBackgroundApps(activity, true, appsAutoRefresh) { forceMenuVisibilityUpdate(activity) }
    }
  }

  fun loadBackgroundApps(
    activity: MainActivity,
    showRefreshIndicator: Boolean = true,
    appsAutoRefresh: Boolean = false,
    onMenuVisibilityUpdated: () -> Unit,
  ) {
    Log.d(TAG, "loadBackgroundApps requested showRefreshIndicator=$showRefreshIndicator, isLoading=$isLoadingBackgroundApps, currentListSize=${appsDataList.size}, hasPermission=$hasPermission")

    if (showRefreshIndicator && isLoadingBackgroundApps) {
      Log.d(TAG, "loadBackgroundApps skipped because visible refresh is already loading")
      return
    }

    if (!shellManager.hasAnyShellPermission()) {
      Log.w(TAG, "loadBackgroundApps no shell permission, updating permission UI")
      updatePermissionUi(activity, appsAutoRefresh = appsAutoRefresh)
      return
    }

    if (showRefreshIndicator) {
      isLoadingBackgroundApps = true
    }

    appManager.loadBackgroundApps { result ->
      Log.d(TAG, "loadBackgroundApps callback resultSize=${result.size}, oldListSize=${appsDataList.size}, showRefreshIndicator=$showRefreshIndicator")

      if (showRefreshIndicator) {
        isLoadingBackgroundApps = false
      }

      if (result.isEmpty() && appsDataList.isEmpty() && backgroundLoadRetryCount < 5) {
        backgroundLoadRetryCount++
        handler.postDelayed({ loadBackgroundApps(activity, showRefreshIndicator, appsAutoRefresh, onMenuVisibilityUpdated) }, 700)
        return@loadBackgroundApps
      }

      backgroundLoadRetryCount = 0
      val selectedPackages = appsDataList.filter { it.isSelected }.map { it.packageName }.toSet()

      val updatedResult = result.map { app ->
        if (selectedPackages.contains(app.packageName)) {
          app.copy(isSelected = true)
        } else {
          app
        }
      }

      appsDataList.clear()
      appsDataList.addAll(updatedResult)

      sortAppsDataList(activity)
      onMenuVisibilityUpdated()
    }
  }

  fun replaceAllSelection(activity: MainActivity, selected: Boolean) {
    val updated = appsDataList.map { app -> if (!app.isProtected) app.copy(isSelected = selected) else app }
    appsDataList.clear()
    appsDataList.addAll(updated)
  }

  fun replaceApp(activity: MainActivity, app: AppModel) {
    val index = appsDataList.indexOfFirst { it.packageName == app.packageName }
    if (index >= 0) appsDataList[index] = app.copy()
  }

  fun sortAppsDataList(activity: MainActivity) {
    val prefs = activity.getSharedPreferences(PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
    val sortMode = prefs.getString(AppsListPreferences.KEY_SORT_MODE, AppsListPreferences.SORT_BY_NAME)
    val descending = prefs.getBoolean(AppsListPreferences.KEY_SORT_DESCENDING, false)

    val sorted = AppSortUtils.sortApps(appsDataList, sortMode, descending)
    appsDataList.clear()
    appsDataList.addAll(sorted)
  }

  fun updatePermissionUi(activity: MainActivity, forceRefresh: Boolean = false, appsAutoRefresh: Boolean = false) {
    val previous = hasPermission
    hasPermission = shellManager.hasAnyShellPermission()
    Log.d(TAG, "updatePermissionUi previous=$previous, current=$hasPermission")

    if (hasPermission) {
      val permissionJustGranted = !previous && hasPermission
      if (forceRefresh || permissionJustGranted || appsDataList.isEmpty() || appsAutoRefresh) {
        loadBackgroundApps(
          activity = activity,
          showRefreshIndicator = forceRefresh || appsDataList.isEmpty(),
          appsAutoRefresh = appsAutoRefresh,
          onMenuVisibilityUpdated = { forceMenuVisibilityUpdate(activity) },
        )
      }
    }
  }

  fun forceMenuVisibilityUpdate(activity: MainActivity) {
    val updated = appsDataList.toList()
    appsDataList.clear()
    appsDataList.addAll(updated)
  }
  fun setupAutoRefresh(activity: MainActivity) {
    val prefs = activity.getSharedPreferences(com.yassernull.shappky.core.preferences.PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)

    appsAutoRefresh = prefs.getBoolean(AppsListPreferences.KEY_APPS_AUTO_REFRESH, true)
    val appsAutoRefreshIntervalMs = prefs.getLong(AppsListPreferences.KEY_APPS_AUTO_REFRESH_INTERVAL_MS, AppsListPreferences.DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS).coerceAtLeast(1000L)

    autoRefreshManager.updateAppsAutoRefresh(appsAutoRefresh, appsAutoRefreshIntervalMs) {
      loadBackgroundApps(
        activity = activity,
        showRefreshIndicator = false,
        appsAutoRefresh = appsAutoRefresh,
        onMenuVisibilityUpdated = { },
      )
    }
  }
}
