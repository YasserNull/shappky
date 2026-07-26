package com.yassernull.shappky.utils

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.data.models.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun PackageManager.getLauncherPackage(): String? {
  val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
  return resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
}

fun Context.getKeyboardPackage(): String? {
  val raw = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
  return if (raw != null && raw.contains("/")) raw.split("/")[0] else null
}

fun Context.getWallpaperPackages(pm: PackageManager): Set<String> {
  val packages = mutableSetOf<String>()
  try {
    val wm = WallpaperManager.getInstance(this)
    wm.wallpaperInfo?.packageName?.let { packages.add(it) }
    if (Build.VERSION.SDK_INT >= 34) {
      wm.getWallpaperInfo(WallpaperManager.FLAG_LOCK)?.packageName?.let { packages.add(it) }
    }
  } catch (_: Exception) {
  }
  if (packages.isEmpty()) {
    try {
      val wallpaperIntent = Intent("android.service.wallpaper.WallpaperService")
      val wallpaperServices = pm.queryIntentServices(wallpaperIntent, PackageManager.GET_META_DATA)
      for (service in wallpaperServices) {
        service.serviceInfo.packageName?.let { packages.add(it) }
      }
    } catch (_: Exception) {
    }
  }
  return packages
}

fun applyRegexToSelectedPackages(
  regexStr: String,
  allApps: List<AppModel>,
  selectedPackages: Set<String>,
): Set<String> {
  if (regexStr.isBlank() || allApps.isEmpty()) return selectedPackages
  val patterns = regexStr.split("|").map { it.trim() }.filter { it.isNotEmpty() }
  val matchingApps = allApps.filter { app ->
    patterns.any { pattern ->
      try {
        val regex = pattern.replace(".", "\\.").replace("*", ".*").toRegex()
        regex.matches(app.packageName)
      } catch (_: Exception) {
        (pattern.endsWith(".*") && app.packageName.startsWith(pattern.removeSuffix(".*"))) ||
          app.packageName == pattern
      }
    }
  }.map { it.packageName }
  return if (matchingApps.isNotEmpty()) selectedPackages + matchingApps else selectedPackages
}

fun getAndroidPackages(allApps: List<AppModel>): List<String> = allApps
  .filter {
    it.packageName.startsWith("com.android.") ||
      it.packageName.startsWith("android.") ||
      it.packageName == "android"
  }
  .map { it.packageName }

suspend fun Context.collectActiveWidgetPackages(shellManager: ShellManager): Set<String> = withContext(Dispatchers.IO) {
  try {
    val output = shellManager.runShellCommandAndGetFullOutput("dumpsys appwidget") ?: ""
    val activePackages = mutableSetOf<String>()
    val regex = Regex("cmp:ComponentInfo\\{([^/]+)/")
    val legacyRegex = Regex("provider=ComponentInfo\\{([^/]+)/")

    var inWidgetsSection = false
    for (line in output.split('\n')) {
      val trimmed = line.trim()
      if (trimmed == "Widgets:" || trimmed == "AppWidgetIds:") {
        inWidgetsSection = true
        continue
      } else if (line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) {
        if (inWidgetsSection && !line.contains("Widgets") && !line.contains("AppWidgetIds")) {
          inWidgetsSection = false
        }
      }
      if (inWidgetsSection) {
        val match = regex.find(line) ?: legacyRegex.find(line)
        if (match != null) {
          activePackages.add(match.groupValues[1])
        }
      }
    }
    activePackages
  } catch (_: Exception) {
    emptySet()
  }
}
