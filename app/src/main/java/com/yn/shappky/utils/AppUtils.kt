package com.yn.shappky.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.yn.shappky.data.models.AppModel
import java.util.concurrent.Executors

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
      val protectedApps = ProtectionManager.getProtectedApps(this)
      val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      val allApps = mutableListOf<AppModel>()
      for (appInfo in packages) {
        if (appInfo.packageName == packageName) continue
        val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
        val label = pm.getApplicationLabel(appInfo).toString()
        val pkg = appInfo.packageName
        val isProtected = pkg == packageName || protectedApps.contains(pkg)

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
