package com.yassernull.shappky.ui.activities.main.logic

import android.util.Log
import androidx.compose.runtime.Composable
import com.yassernull.shappky.core.managers.RamState
import com.yassernull.shappky.core.preferences.AppsListPreferences
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.ui.activities.main.MainActivity
import com.yassernull.shappky.ui.components.RamUsageBar

@Composable
fun AppsRamUsage(
  ramState: RamState,
) {
  RamUsageBar(ramState)
}

object AppsRamUsageLogic {
  private const val TAG = "AppsRamUsageLogic"

  fun refreshAppsRamUsage(activity: MainActivity) {
    Log.d(TAG, "refreshAppsRamUsage requested hasPermission=${AppsListLogic.hasPermission}, listSize=${AppsListLogic.appsDataList.size}")
    if (!AppsListLogic.hasPermission || AppsListLogic.appsDataList.isEmpty()) {
      return
    }

    AppsListLogic.appManager.loadAppsRamUsage(AppsListLogic.appsDataList.map { it.packageName }) { ramUsageByPackage ->
      if (ramUsageByPackage.isNotEmpty()) {
        val updatedApps = AppsListLogic.appsDataList.map { app ->
          val newRamKb = ramUsageByPackage[app.packageName] ?: app.ramKb
          app.copy(
            ramKb = newRamKb,
            appRam = if (newRamKb > 0) AppsListLogic.appManager.formatMemorySize(newRamKb) else app.appRam,
          )
        }
        AppsListLogic.appsDataList.clear()
        AppsListLogic.appsDataList.addAll(updatedApps)

        val prefs = activity.getSharedPreferences(com.yassernull.shappky.core.preferences.PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
        if (prefs.getString(com.yassernull.shappky.core.preferences.AppsListPreferences.KEY_SORT_MODE, com.yassernull.shappky.core.preferences.AppsListPreferences.SORT_BY_NAME) == com.yassernull.shappky.core.preferences.AppsListPreferences.SORT_BY_RAM) {
          AppsListLogic.sortAppsDataList(activity)
        }
      }
    }
  }
}
