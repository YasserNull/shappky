package com.yn.shappky.ui.activities.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHandler {
  const val NOTIFICATION_PERMISSION_CODE = 1
  fun checkAndRequestNotificationPermission(activity: ComponentActivity): Boolean {
    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) !=
      PackageManager.PERMISSION_GRANTED

    if (needsPermission) {
      ActivityCompat.requestPermissions(
        activity,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        NOTIFICATION_PERMISSION_CODE,
      )
      return true
    }
    return false
  }

  fun hasNotificationPermission(context: android.content.Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
  } else {
    true
  }

  fun createShizukuPermissionListener(
    activity: ComponentActivity,
    onGranted: () -> Unit,
  ): rikka.shizuku.Shizuku.OnRequestPermissionResultListener = rikka.shizuku.Shizuku.OnRequestPermissionResultListener { _, grantResult ->
    if (grantResult == PackageManager.PERMISSION_GRANTED) {
      onGranted()
    }
    com.yn.shappky.utils.PermissionUtils.checkAndRequestBatteryOptimization(activity)
  }

  fun setupShizukuPermissionListener(
    activity: com.yn.shappky.ui.activities.main.MainActivity,
    shellManager: com.yn.shappky.utils.ShellManager,
  ) {
    val listener = createShizukuPermissionListener(activity) {
      com.yn.shappky.ui.activities.main.logic.AppsListLogic.loadBackgroundApps(
        activity = activity,
        showRefreshIndicator = true,
        appsAutoRefresh = com.yn.shappky.ui.activities.main.logic.AppsListLogic.appsAutoRefresh,
        onMenuVisibilityUpdated = { com.yn.shappky.ui.activities.main.logic.AppsListLogic.forceMenuVisibilityUpdate(activity) },
      )
    }
    shellManager.setShizukuPermissionListener(listener)
  }

  fun handlePermissionsResult(
    activity: ComponentActivity,
    requestCode: Int,
    shellManager: com.yn.shappky.utils.ShellManager,
  ) {
    if (requestCode == NOTIFICATION_PERMISSION_CODE) {
      val mode = activity.getSharedPreferences(com.yn.shappky.core.preferences.PREFERENCES_NAME, Context.MODE_PRIVATE).getString("permission_mode", "auto") ?: "auto"
      com.yn.shappky.utils.PermissionUtils.checkAndRequestShizukuFlow(activity, mode, shellManager)
    }
  }
}
