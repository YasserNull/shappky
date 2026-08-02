package com.yassernull.shappky.services

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class ShappkyActionInput {
  @field:TaskerInputField("action_type")
  var actionType: String = "START_SERVICE"

  @field:TaskerInputField("trigger_id")
  var triggerId: String? = null
}
