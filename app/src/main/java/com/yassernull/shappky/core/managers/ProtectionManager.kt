package com.yassernull.shappky.core.managers

import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
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
  private const val GROUP_PROTECTED_CACHE_TTL_MS = 5000L

  private val groupProtectedCache = mutableMapOf<String, Pair<Long, Set<String>>>()

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
    if (getEnabledGroupProtectedPackages(context).contains(packageName)) return true
    return isAppProtectedByRegex(context, packageName) &&
      !getProtectedAppsExemptions(context).contains(packageName)
  }

  fun getEffectiveProtectedApps(context: Context): Set<String> = getProtectedApps(context) + getEnabledGroupProtectedPackages(context)

  fun refreshSpecialProtectedApps(context: Context) {
    try {
      val current = getProtectedApps(context).toMutableSet()
      var changed = false

      if (getGroupEnabled(context, "launcher")) {
        try {
          val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
          context.packageManager.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
            ?.let { changed = current.add(it) || changed }
        } catch (e: Exception) {
          Log.e(TAG, "Error refreshing default launcher", e)
        }
      }

      if (getGroupEnabled(context, "keyboard")) {
        try {
          var keyboard = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
          if (keyboard != null && keyboard.contains("/")) {
            changed = current.add(keyboard.split("/")[0]) || changed
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error refreshing default keyboard", e)
        }
      }

      if (getGroupEnabled(context, "wallpaper")) {
        try {
          val wallpaperManager = WallpaperManager.getInstance(context)
          wallpaperManager.wallpaperInfo?.packageName?.let { changed = current.add(it) || changed }
          if (Build.VERSION.SDK_INT >= 34) {
            wallpaperManager.getWallpaperInfo(WallpaperManager.FLAG_LOCK)?.packageName?.let { changed = current.add(it) || changed }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error refreshing default wallpaper", e)
        }
      }

      if (getGroupEnabled(context, "widgets")) {
        try {
          AppWidgetManager.getInstance(context).installedProviders.forEach { provider ->
            changed = current.add(provider.provider.packageName) || changed
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error refreshing widget providers", e)
        }
      }

      if (changed) {
        saveProtectedApps(context, current)
        groupProtectedCache.remove(context.packageName)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error refreshing special protected apps", e)
    }
  }

  fun getEnabledGroupProtectedPackages(context: Context): Set<String> {
    val now = System.currentTimeMillis()
    val cached = groupProtectedCache[context.packageName]
    if (cached != null && now - cached.first < GROUP_PROTECTED_CACHE_TTL_MS) {
      return cached.second
    }
    val result = computeEnabledGroupProtectedPackages(context)
    groupProtectedCache[context.packageName] = now to result
    return result
  }

  private fun computeEnabledGroupProtectedPackages(context: Context): Set<String> {
    val pm = context.packageManager
    val result = mutableSetOf<String>()

    if (getGroupEnabled(context, "launcher")) {
      try {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        pm.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let { result.add(it) }
      } catch (e: Exception) {
        Log.e(TAG, "Error resolving default launcher", e)
      }
    }

    if (getGroupEnabled(context, "keyboard")) {
      try {
        var keyboard = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        if (keyboard != null && keyboard.contains("/")) {
          result.add(keyboard.split("/")[0])
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error getting default keyboard", e)
      }
    }

    if (getGroupEnabled(context, "wallpaper")) {
      try {
        val wallpaperManager = WallpaperManager.getInstance(context)
        wallpaperManager.wallpaperInfo?.packageName?.let { result.add(it) }
        if (Build.VERSION.SDK_INT >= 34) {
          wallpaperManager.getWallpaperInfo(WallpaperManager.FLAG_LOCK)?.packageName?.let { result.add(it) }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error getting default wallpaper", e)
      }
    }

    if (getGroupEnabled(context, "widgets")) {
      try {
        AppWidgetManager.getInstance(context).installedProviders.forEach { provider ->
          result.add(provider.provider.packageName)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error listing widget providers", e)
      }
    }

    val persistentEnabled = getGroupEnabled(context, "persistent")
    val androidEnabled = getGroupEnabled(context, "android")
    val googleEnabled = getGroupEnabled(context, "google")
    if (persistentEnabled || androidEnabled || googleEnabled) {
      try {
        val packages = pm.getInstalledApplications(0)
        for (appInfo in packages) {
          val pkg = appInfo.packageName
          if (persistentEnabled && appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0) result.add(pkg)
          if (androidEnabled && (pkg == "android" || pkg.startsWith("com.android.") || pkg.startsWith("android."))) result.add(pkg)
          if (googleEnabled && pkg.startsWith("com.google.android.")) result.add(pkg)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error listing installed packages for group protection", e)
      }
    }

    return result
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

    // com.android.*, android.*, Google Android services, and persistent apps
    try {
      val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
      for (appInfo in packages) {
        val pkg = appInfo.packageName
        val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
        if (pkg == "android" || pkg.startsWith("com.android.") || pkg.startsWith("android.") || pkg.startsWith("com.google.android.") || isPersistent) {
          defaultSet.add(pkg)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error listing installed packages for default protection", e)
    }

    // Active widget provider apps
    try {
      val providers = AppWidgetManager.getInstance(context).installedProviders
      for (provider in providers) {
        defaultSet.add(provider.provider.packageName)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error listing widget providers for default protection", e)
    }

    return defaultSet
  }
}
