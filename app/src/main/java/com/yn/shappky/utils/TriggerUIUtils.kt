package com.yn.shappky.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R
import com.yn.shappky.data.models.TriggerModel

@Composable
fun buildTriggerSummary(trigger: TriggerModel): String {
  val categories = mutableListOf<String>()
  if (trigger.selectUserApps) categories.add(stringResource(R.string.user_apps))
  if (trigger.selectSystemApps) categories.add(stringResource(R.string.system_apps))
  if (trigger.selectPersistentApps) categories.add(stringResource(R.string.persistent_apps))

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
    append(if (trigger.excludedApps.isNotEmpty()) trigger.excludedApps.size else 0)
    append(" | ")
    append(stringResource(R.string.manually_select_apps))
    append(": ")
    append(if (trigger.manuallySelectedApps.isNotEmpty()) trigger.manuallySelectedApps.size else 0)
  }
}
