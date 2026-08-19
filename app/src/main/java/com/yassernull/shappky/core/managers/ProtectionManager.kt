package com.yassernull.shappky.core.managers

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log

object ProtectionManager {
  private const val TAG = "ProtectionManager"
  private const val KEY_PROTECTED_APPS = "protectedApps"
  private const val KEY_PROTECTED_REGEX = "protectedRegex"
  private const val KEY_PROTECTED_EXEMPTIONS = "protectedAppsExemptions"
  private const val KEY_GROUP_PREFIX = "group_"
  private const val PREFERENCES_NAME = "AppPreferences"

  fun getProtectedApps(context: Context): Set<String> {
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    if (sharedPrefs.contains(KEY_PROTECTED_APPS)) {
      val savedSet = sharedPrefs.getStringSet(KEY_PROTECTED_APPS, null)
      if (savedSet != null) {
        return savedSet
      }
    }
    return getDefaultProtectedApps(context)
  }

  fun saveProtectedApps(context: Context, apps: Set<String>) {
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putStringSet(KEY_PROTECTED_APPS, apps).apply()
    Log.d(TAG, "Saved protected apps count=${apps.size}")
  }

  fun getProtectedRegex(context: Context): String {
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getString(KEY_PROTECTED_REGEX, "") ?: ""
  }

  fun saveProtectedRegex(context: Context, regex: String) {
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putString(KEY_PROTECTED_REGEX, regex).apply()
  }

  fun getProtectedAppsExemptions(context: Context): Set<String> {
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getStringSet(KEY_PROTECTED_EXEMPTIONS, null) ?: emptySet()
  }

  fun saveProtectedAppsExemptions(context: Context, apps: Set<String>) {
    val sharedPrefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putStringSet(KEY_PROTECTED_EXEMPTIONS, apps).apply()
  }

  fun getGroupEnabled(context: Context, group: String): Boolean = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    .getBoolean(KEY_GROUP_PREFIX + group + "_enabled", true)

  fun getGroupMembers(context: Context, group: String): Set<String> = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    .getStringSet(KEY_GROUP_PREFIX + group + "_members", null) ?: emptySet()

  fun saveGroupState(context: Context, group: String, enabled: Boolean, members: Set<String>) {
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_GROUP_PREFIX + group + "_enabled", enabled)
      .putStringSet(KEY_GROUP_PREFIX + group + "_members", members)
      .apply()
  }

  fun isPackageProtected(context: Context, packageName: String): Boolean {
    if (getProtectedApps(context).contains(packageName)) return true
    return isAppProtectedByRegex(context, packageName) &&
      !getProtectedAppsExemptions(context).contains(packageName)
  }

  fun isAppProtectedByRegex(context: Context, packageName: String): Boolean {
    val regexStr = getProtectedRegex(context)
    if (regexStr.isBlank()) return false

    val patterns = regexStr.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    for (pattern in patterns) {
      try {
        val regex = pattern.replace(".", "\\.").replace("*", ".*").toRegex()
        if (regex.matches(packageName)) return true
      } catch (e: Exception) {
        // Fallback for simple startsWith if regex compilation fails
        if (pattern.endsWith(".*") && packageName.startsWith(pattern.removeSuffix(".*"))) {
          return true
        } else if (packageName == pattern) {
          return true
        }
      }
    }
    return false
  }

  fun getDefaultProtectedApps(context: Context): Set<String> {
    val pm = context.packageManager
    val defaultSet = mutableSetOf<String>()

    // Self
    defaultSet.add(context.packageName)

    // Keyboard
    try {
      var keyboard = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
      if (keyboard != null && keyboard.contains("/")) {
        keyboard = keyboard.split("/")[0]
        defaultSet.add(keyboard)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting default keyboard", e)
    }

    // Launcher
    try {
      val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
      val resolveInfo = pm.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
      resolveInfo?.activityInfo?.packageName?.let { defaultSet.add(it) }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting default launcher", e)
    }

    // Wallpaper
    try {
      val wallpaperManager = WallpaperManager.getInstance(context)
      wallpaperManager.wallpaperInfo?.packageName?.let { defaultSet.add(it) }
      if (Build.VERSION.SDK_INT >= 34) {
        wallpaperManager.getWallpaperInfo(WallpaperManager.FLAG_LOCK)?.packageName?.let { defaultSet.add(it) }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting default wallpaper", e)
    }

    // com.android.*, android.*, and Google Android services
    try {
      val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      for (appInfo in packages) {
        val pkg = appInfo.packageName
        if (pkg == "android" || pkg.startsWith("com.android.") || pkg.startsWith("android.") || pkg.startsWith("com.google.android.")) {
          defaultSet.add(pkg)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error listing installed packages for default protection", e)
    }

    return defaultSet
  }
}
