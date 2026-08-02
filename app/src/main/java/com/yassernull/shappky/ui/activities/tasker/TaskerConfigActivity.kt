package com.yassernull.shappky.ui.activities.tasker

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.services.ShappkyActionInput
import com.yassernull.shappky.services.ShappkyActionRunner
import com.yassernull.shappky.ui.activities.tasker.events.handleOnCreate

class TaskerConfigActivity :
  ComponentActivity(),
  TaskerPluginConfig<ShappkyActionInput> {

  override val context: Context get() = this

  internal val taskerHelper by lazy {
    object : TaskerPluginConfigHelperNoOutput<ShappkyActionInput, ShappkyActionRunner>(this) {
      override val inputClass get() = ShappkyActionInput::class.java
      override val runnerClass get() = ShappkyActionRunner::class.java
    }
  }

  internal var actionType by mutableStateOf("START_SERVICE")
  internal var triggerId by mutableStateOf<String?>(null)

  internal var availableTriggers by mutableStateOf<List<TriggerModel>>(emptyList())

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleOnCreate(savedInstanceState)
  }

  override val inputForTasker: TaskerInput<ShappkyActionInput>
    get() {
      Log.d("TaskerConfigActivity", "getInputForTasker called -> Action: $actionType, Trigger: $triggerId")
      val input = ShappkyActionInput()
      input.actionType = actionType
      input.triggerId = triggerId
      return TaskerInput(input)
    }

  override fun assignFromInput(input: TaskerInput<ShappkyActionInput>) {
    actionType = input.regular.actionType
    triggerId = input.regular.triggerId
    Log.d("TaskerConfigActivity", "assignFromInput called -> Assigned Action: $actionType, Trigger: $triggerId")
  }
}
