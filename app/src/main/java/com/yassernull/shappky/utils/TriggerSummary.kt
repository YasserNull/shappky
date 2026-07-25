package com.yassernull.shappky.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.TriggerModel

@Composable
fun TriggerModel.buildTriggerSummary(): String {
  val categories = mutableListOf<String>()
  if (selectUserApps) categories.add(stringResource(R.string.user_apps))
  if (selectSystemApps) categories.add(stringResource(R.string.system_apps))
  if (selectPersistentApps) categories.add(stringResource(R.string.persistent_apps))

  val catStr = if (categories.isNotEmpty()) {
    categories.joinToString(", ")
  } else {
    "-"
  }

  return buildString {
    append(stringResource(R.string.selected))
    append(": ")
    append(catStr)
    append(" | ")
    append(stringResource(R.string.exclude_apps))
    append(": ")
    append(if (excludedApps.isNotEmpty()) excludedApps.size else 0)
    append(" | ")
    append(stringResource(R.string.manually_select_apps))
    append(": ")
    append(if (manuallySelectedApps.isNotEmpty()) manuallySelectedApps.size else 0)
  }
}
