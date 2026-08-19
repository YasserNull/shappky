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

suspend fun Context.collectCurrentWallpaperPackages(shellManager: ShellManager): Set<String> = withContext(Dispatchers.IO) {
  val context = this@collectCurrentWallpaperPackages
  val result = mutableSetOf<String>()
  try {
    val wallpaperManager = WallpaperManager.getInstance(context)
    wallpaperManager.wallpaperInfo?.packageName?.let { result.add(it) }
    if (Build.VERSION.SDK_INT >= 34) {
      wallpaperManager.getWallpaperInfo(WallpaperManager.FLAG_LOCK)?.packageName?.let { result.add(it) }
    }
  } catch (_: Exception) {
  }
  try {
    val command = "dumpsys wallpaper | grep \"mWallpaperComponent=\" | head -1 | sed 's/.*ComponentInfo{//' | cut -d'/' -f1"
    val output = shellManager.runShellCommandAndGetFullOutput(command)
    if (output != null) {
      result.addAll(
        output
          .split('\n')
          .map { it.trim() }
          .filter { it.isNotEmpty() && !it.startsWith("ERROR:") },
      )
    }
  } catch (_: Exception) {
  }
  result
}

suspend fun Context.collectActiveWidgetPackages(shellManager: ShellManager): Set<String> = withContext(Dispatchers.IO) {
  try {
    val command = "dumpsys appwidget | awk '/^Widgets:/{flag=1; next} flag && !/^[A-Z]/{print}' | grep \"cmp:ComponentInfo\" | sed 's/.*cmp:ComponentInfo{//' | cut -d'/' -f1 | sort -u"
    val output = shellManager.runShellCommandAndGetFullOutput(command) ?: return@withContext emptySet<String>()
    output
      .split('\n')
      .map { it.trim() }
      .filter { it.isNotEmpty() && !it.startsWith("ERROR:") }
      .toSet()
  } catch (_: Exception) {
    emptySet()
  }
}
