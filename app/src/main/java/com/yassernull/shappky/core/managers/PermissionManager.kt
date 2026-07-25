package com.yassernull.shappky.core.managers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import rikka.shizuku.Shizuku

object PermissionManager {

  fun checkAndRequestShizukuFlow(
    context: Context,
    permissionMode: String,
    shellManager: ShellManager,
  ) {
    val ping = Shizuku.pingBinder()
    val needsShizukuRequest = (permissionMode == "shizuku" || permissionMode == "auto") &&
      ping &&
      Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED

    shellManager.checkShellPermissions()

    if (!needsShizukuRequest) {
      checkAndRequestBatteryOptimization(context)
    }
  }

  fun checkAndRequestBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
      if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
        try {
          val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
          }
          context.startActivity(intent)
        } catch (e: Exception) {
          try {
            val intentSettings = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intentSettings)
          } catch (ex: Exception) {
            // Ignored
          }
        }
      }
    }
  }
}
