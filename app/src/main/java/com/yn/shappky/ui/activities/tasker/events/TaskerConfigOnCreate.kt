package com.yn.shappky.ui.activities.tasker.events

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import com.yn.shappky.core.preferences.ShappkyTheme
import com.yn.shappky.ui.activities.tasker.TaskerConfigActivity
import com.yn.shappky.ui.activities.tasker.TaskerConfigContent
import com.yn.shappky.utils.TriggerManager

fun TaskerConfigActivity.handleOnCreate(savedInstanceState: Bundle?) {
  Log.d("TaskerConfigActivity", "onCreate called.")
  taskerHelper.onCreate()

  availableTriggers = TriggerManager.getTriggers(this)
  Log.d("TaskerConfigActivity", "Loaded ${availableTriggers.size} triggers.")

  setContent {
    ShappkyTheme {
      TaskerConfigContent(
        actionType = actionType,
        triggerId = triggerId,
        availableTriggers = availableTriggers,
        onActionTypeChange = { actionType = it },
        onTriggerIdChange = { triggerId = it },
        onSave = {
          Log.d("TaskerConfigActivity", "Save clicked. ActionType: $actionType, TriggerId: $triggerId")
          val result = taskerHelper.finishForTasker()
          Log.d("TaskerConfigActivity", "finishForTasker result: $result")
        },
        onBack = { finish() },
      )
    }
  }
}
