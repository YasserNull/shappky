package com.yassernull.shappky.ui.activities.addTrigger

import android.app.Activity
import android.widget.Toast
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.core.managers.TriggerServiceManager
import com.yassernull.shappky.data.models.TriggerModel

object AddTriggerActions {
  fun saveTrigger(
    trigger: TriggerModel,
    activity: Activity,
  ) {
    if (trigger.name.trim().isEmpty()) {
      Toast.makeText(activity, activity.getString(R.string.trigger_name_empty), Toast.LENGTH_SHORT).show()
      return
    }

    TriggerManager.addOrUpdateTrigger(activity, trigger)
    TriggerServiceManager.updateTriggerServiceState(activity)
    activity.finish()
  }
}
