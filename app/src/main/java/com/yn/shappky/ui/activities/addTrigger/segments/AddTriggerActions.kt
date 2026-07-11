package com.yn.shappky.ui.activities.addTrigger

import android.app.Activity
import android.widget.Toast
import com.yn.shappky.R
import com.yn.shappky.data.models.TriggerModel
import com.yn.shappky.utils.TriggerManager
import com.yn.shappky.utils.TriggerServiceManager

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
