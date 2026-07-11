package com.yn.shappky.utils

import com.yn.shappky.core.preferences.AppsListPreferences.SORT_BY_RAM
import com.yn.shappky.core.preferences.AppsListPreferences.SORT_BY_TYPE
import com.yn.shappky.data.models.AppModel
import java.util.Locale

object AppSortUtils {

  fun sortApps(
    appsList: List<AppModel>,
    sortMode: String?,
    descending: Boolean,
  ): List<AppModel> {
    val appTypeComparator = compareBy<AppModel> { it.isSystemApp }.thenBy { it.isPersistentApp }

    val comparator = when (sortMode) {
      SORT_BY_RAM -> {
        val ramComparator = if (descending) {
          compareByDescending<AppModel> { it.ramKb }
        } else {
          compareBy { it.ramKb }
        }
        appTypeComparator.then(ramComparator).thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName }
      }
      SORT_BY_TYPE -> {
        val typeComparator = if (descending) {
          compareByDescending<AppModel> { getTypePriority(it) }
        } else {
          compareBy<AppModel> { getTypePriority(it) }
        }
        typeComparator.thenBy { it.appName.lowercase(Locale.getDefault()) }
      }
      else -> {
        val nameComparator = if (descending) {
          compareByDescending<AppModel> { it.appName.lowercase(Locale.getDefault()) }
        } else {
          compareBy { it.appName.lowercase(Locale.getDefault()) }
        }
        appTypeComparator.then(nameComparator)
      }
    }

    return appsList.sortedWith(comparator)
  }

  private fun getTypePriority(app: AppModel): Int = when {
    app.isProtected -> 4
    app.isPersistentApp -> 3
    app.isSystemApp -> 2
    else -> 1 // User app
  }
}
