package com.yassernull.shappky.core.managers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.yassernull.shappky.data.models.AppModel

object AppModelFilter {

  fun buildRunningAppModels(
    runningEntries: Set<String>,
    hiddenApps: Set<String>,
    protectedApps: Set<String>,
    showUserApps: Boolean,
    showSystemApps: Boolean,
    showPersistentApps: Boolean,
    showProtectedApps: Boolean,
    context: Context,
    formatMemorySize: (Long) -> String,
  ): List<AppModel> {
    val pm = context.packageManager
    val result = mutableListOf<AppModel>()
    for (packageEntry in runningEntries) {
      val parts = packageEntry.split(":")
      val packageName = parts[0]
      val ramUsage = parts.getOrNull(1)?.toLongOrNull() ?: 0L
      val cpuUsage = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

      try {
        if (hiddenApps.contains(packageName)) continue

        val isProtected = protectedApps.contains(packageName) ||
          ProtectionManager.isAppProtectedByRegex(context, packageName)

        val appInfo = pm.getApplicationInfo(packageName, 0)
        val isPersistentApp = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
        val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val label = pm.getApplicationLabel(appInfo).toString()

        if (!showSystemApps && isSystemApp) continue
        if (!showPersistentApps && isPersistentApp) continue
        if (!showProtectedApps && isProtected) continue
        if (!showUserApps && !isSystemApp && !isPersistentApp) continue

        result.add(
          AppModel(
            appName = label,
            packageName = packageName,
            appRam = formatMemorySize(ramUsage),
            ramKb = ramUsage,
            appCpu = String.format(java.util.Locale.US, "%.1f%%", cpuUsage),
            appIcon = pm.getApplicationIcon(appInfo),
            isSystemApp = isSystemApp,
            isPersistentApp = isPersistentApp,
            isProtected = isProtected,
          ),
        )
      } catch (_: PackageManager.NameNotFoundException) {
      } catch (_: Exception) {
      }
    }
    return result
  }

  fun buildAllAppsList(
    context: Context,
    protectedApps: Set<String>,
  ): List<AppModel> {
    val pm = context.packageManager
    val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    val allApps = mutableListOf<AppModel>()
    for (appInfo in packages) {
      val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
      val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
      val label = pm.getApplicationLabel(appInfo).toString()
      val pkg = appInfo.packageName
      val isProtected = protectedApps.contains(pkg) ||
        ProtectionManager.isAppProtectedByRegex(context, pkg)

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
    return allApps
  }
}
