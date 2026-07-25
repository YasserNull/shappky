package com.yassernull.shappky.core.domain.trackers

import android.util.Log

class AppForegroundTracker {
  companion object {
    private const val TAG = "AppForegroundTracker"
  }

  var lastForegroundApp: String? = null
  val lastForegroundTimeMap = mutableMapOf<String, Long>()

  fun getForegroundPackage(dumpOutput: String): String? {
    val lines = dumpOutput.split("\n")

    // 1. Try ResumedActivity lines first
    for (line in lines) {
      if (line.contains("ResumedActivity") || line.contains("mResumedActivity") || line.contains("topResumedActivity")) {
        val match = Regex("([a-zA-Z_][a-zA-Z0-9_\\.]*)/").find(line)
        if (match != null) {
          val pkg = match.groupValues[1].trim()
          if (pkg.contains(".") && !pkg.contains(" ")) {
            Log.d(TAG, "getForegroundPackage: Found ResumedActivity package=$pkg")
            return pkg
          }
        }
      }
    }

    // 2. Fallback to Focus/FocusedApp/FocusedWindow lines
    for (line in lines) {
      if (line.contains("mCurrentFocus") || line.contains("mFocusedApp") || line.contains("mFocusedWindow")) {
        val match = Regex("([a-zA-Z_][a-zA-Z0-9_\\.]*)/").find(line)
        if (match != null) {
          val pkg = match.groupValues[1].trim()
          if (pkg.contains(".") && !pkg.contains(" ")) {
            Log.d(TAG, "getForegroundPackage: Found Focus/Fallback package=$pkg")
            return pkg
          }
        }
      }
    }

    Log.d(TAG, "getForegroundPackage: No matching foreground package found")
    return null
  }

  fun updateForegroundApp(currentForeground: String?, now: Long): String? {
    if (currentForeground != null) {
      lastForegroundTimeMap[currentForeground] = now
      if (currentForeground != lastForegroundApp) {
        val previouslyForeground = lastForegroundApp
        lastForegroundApp = currentForeground
        return previouslyForeground
      }
    }
    return null
  }

  fun markAppAsInactive(appPackage: String, now: Long) {
    lastForegroundTimeMap[appPackage] = now
  }

  fun cleanUpOldForegroundRecords(runningPackages: Set<String>, currentForeground: String?) {
    val iterator = lastForegroundTimeMap.iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      if (!runningPackages.contains(entry.key) && entry.key != currentForeground) {
        iterator.remove()
      }
    }
  }

  fun initNewRunningPackages(runningPackages: Set<String>, currentForeground: String?, now: Long) {
    for (pkg in runningPackages) {
      if (pkg != currentForeground && !lastForegroundTimeMap.containsKey(pkg)) {
        lastForegroundTimeMap[pkg] = now
      }
    }
  }

  fun removeRecord(appPackage: String) {
    lastForegroundTimeMap.remove(appPackage)
  }

  fun getLastActiveTime(appPackage: String, now: Long): Long = lastForegroundTimeMap[appPackage] ?: now
}
