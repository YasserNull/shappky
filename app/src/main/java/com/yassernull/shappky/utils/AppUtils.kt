package com.yassernull.shappky.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.yassernull.shappky.core.managers.ProtectionManager
import com.yassernull.shappky.data.models.AppModel
import java.util.concurrent.Executors

fun Context.restartApp() {
  packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
  }
  if (this is Activity) {
    finish()
  }
  Runtime.getRuntime().exit(0)
}

fun Context.getAppName(packageName: String): String = try {
  val pm = packageManager
  val appInfo = pm.getApplicationInfo(packageName, 0)
  pm.getApplicationLabel(appInfo).toString()
} catch (e: Exception) {
  packageName
}

fun Context.loadAllApps(callback: (List<AppModel>) -> Unit) {
  val executor = Executors.newSingleThreadExecutor()
  val handler = Handler(Looper.getMainLooper())
  if (!executor.isShutdown) {
    executor.execute {
      val pm = packageManager
      val protectedApps = ProtectionManager.getEffectiveProtectedApps(this)
      val exemptions = ProtectionManager.getProtectedAppsExemptions(this)
      val regexStr = ProtectionManager.getProtectedRegex(this)
      val patterns = if (regexStr.isNotBlank()) {
        regexStr.split("|").map { it.trim() }.filter { it.isNotEmpty() }.map { pattern ->
          try {
            pattern.replace(".", "\\.").replace("*", ".*").toRegex()
          } catch (_: Exception) {
            null
          } to pattern
        }
      } else {
        emptyList()
      }

      val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      val allApps = mutableListOf<AppModel>()
      for (appInfo in packages) {
        val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
        val label = pm.getApplicationLabel(appInfo).toString()
        val pkg = appInfo.packageName
        val isProtected = protectedApps.contains(pkg) ||
          (!exemptions.contains(pkg) && com.yassernull.shappky.core.managers.AppModelFilter.matchesAnyRegex(pkg, patterns))

        allApps.add(
          AppModel(
            appName = label,
            packageName = pkg,
            appRam = "-",
            ramKb = 0L,
            appIcon = pm.getApplicationIcon(appInfo),
            isSystemApp = isSystem,
            isPersistentApp = isPersistent,
            isProtected = isProtected,
          ),
        )
      }
      allApps.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
      handler.post { callback(allApps) }
      executor.shutdown()
    }
  }
}
