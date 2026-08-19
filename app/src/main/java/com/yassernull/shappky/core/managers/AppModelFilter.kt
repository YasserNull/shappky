package com.yassernull.shappky.core.managers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.yassernull.shappky.data.models.AppModel

object AppModelFilter {
  private const val ICON_CACHE_SIZE = 128

  private val iconCache = LruCache<String, Drawable>(ICON_CACHE_SIZE)

  private fun iconFor(packageName: String, loader: () -> Drawable): Drawable = iconCache.get(packageName) ?: loader().also { iconCache.put(packageName, it) }

  private fun matchesAnyRegex(packageName: String, patterns: List<Pair<Regex?, String>>): Boolean {
    for ((regex, raw) in patterns) {
      if (regex != null) {
        if (regex.matches(packageName)) return true
      } else {
        if (raw.endsWith(".*") && packageName.startsWith(raw.removeSuffix(".*"))) return true
        if (packageName == raw) return true
      }
    }
    return false
  }

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
    val exemptions = ProtectionManager.getProtectedAppsExemptions(context)
    val regexStr = ProtectionManager.getProtectedRegex(context)
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

    val result = mutableListOf<AppModel>()
    for (packageEntry in runningEntries) {
      val parts = packageEntry.split(":")
      val packageName = parts[0]
      val ramUsage = parts.getOrNull(1)?.toLongOrNull() ?: 0L
      val cpuUsage = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0

      try {
        if (hiddenApps.contains(packageName)) continue

        val isProtected = protectedApps.contains(packageName) ||
          (!exemptions.contains(packageName) && matchesAnyRegex(packageName, patterns))

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
            cpuPercent = cpuUsage,
            appIcon = iconFor(appInfo.packageName) { pm.getApplicationIcon(appInfo) },
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
    val exemptions = ProtectionManager.getProtectedAppsExemptions(context)
    val regexStr = ProtectionManager.getProtectedRegex(context)
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

    val allApps = mutableListOf<AppModel>()
    for (appInfo in packages) {
      val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
      val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
      val label = pm.getApplicationLabel(appInfo).toString()
      val pkg = appInfo.packageName
      val isProtected = protectedApps.contains(pkg) ||
        (!exemptions.contains(pkg) && matchesAnyRegex(pkg, patterns))

      allApps.add(
        AppModel(
          appName = label,
          packageName = pkg,
          appRam = "-",
          ramKb = 0L,
          appIcon = iconFor(appInfo.packageName) { pm.getApplicationIcon(appInfo) },
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
