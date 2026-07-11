package com.yn.shappky.ui.activities.main.logic

import android.util.Log
import androidx.compose.runtime.Composable
import com.yn.shappky.core.preferences.AppsListPreferences
import com.yn.shappky.core.preferences.PREFERENCES_NAME
import com.yn.shappky.ui.activities.main.MainActivity
import com.yn.shappky.ui.components.RamUsageBar
import com.yn.shappky.utils.RamState

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

        val prefs = activity.getSharedPreferences(com.yn.shappky.core.preferences.PREFERENCES_NAME, android.content.Context.MODE_PRIVATE)
        if (prefs.getString(com.yn.shappky.core.preferences.AppsListPreferences.KEY_SORT_MODE, com.yn.shappky.core.preferences.AppsListPreferences.SORT_BY_NAME) == com.yn.shappky.core.preferences.AppsListPreferences.SORT_BY_RAM) {
          AppsListLogic.sortAppsDataList(activity)
        }
      }
    }
  }
}
