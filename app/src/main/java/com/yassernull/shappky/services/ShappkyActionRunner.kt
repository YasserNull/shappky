package com.yassernull.shappky.services

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.services.ShappkyService

class ShappkyActionRunner : TaskerPluginRunnerActionNoOutput<ShappkyActionInput>() {
  companion object {
    private const val TAG = "TaskerShappkyRunner"
  }

  override fun run(context: Context, input: TaskerInput<ShappkyActionInput>): TaskerPluginResult<Unit> {
    val actionType = input.regular.actionType
    val triggerId = input.regular.triggerId

    Log.d(TAG, "Tasker triggered Shappky Action! Action Type: $actionType, Trigger ID: $triggerId")

    when (actionType) {
      "START_SERVICE" -> {
        Log.d(TAG, "Starting ShappkyService from Tasker...")
        try {
          ContextCompat.startForegroundService(context, Intent(context, ShappkyService::class.java))
          Log.d(TAG, "Successfully sent start command to ShappkyService.")
        } catch (e: Exception) {
          Log.e(TAG, "Error starting ShappkyService: ${e.message}", e)
          return TaskerPluginResultError(3, context.getString(com.yassernull.shappky.R.string.failed_to_start_service, e.message))
        }
      }
      "STOP_SERVICE" -> {
        Log.d(TAG, "Stopping ShappkyService from Tasker...")
        try {
          context.stopService(Intent(context, ShappkyService::class.java))
          Log.d(TAG, "Successfully sent stop command to ShappkyService.")
        } catch (e: Exception) {
          Log.e(TAG, "Error stopping ShappkyService: ${e.message}", e)
          return TaskerPluginResultError(4, context.getString(com.yassernull.shappky.R.string.failed_to_stop_service, e.message))
        }
      }
      "EXECUTE_TRIGGER" -> {
        Log.d(TAG, "Executing specific trigger from Tasker...")
        if (triggerId != null) {
          val triggers = TriggerManager.getTriggers(context)
          val trigger = triggers.find { it.id == triggerId }
          if (trigger != null) {
            Log.d(TAG, "Trigger found: ${trigger.name}. Executing directly...")
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
            val shellManager = com.yassernull.shappky.core.managers.ShellManager(context, handler, executor)

            if (!shellManager.hasAnyShellPermission()) {
              Log.e(TAG, "Cannot execute trigger: No shell permissions")
              return TaskerPluginResultError(6, context.getString(com.yassernull.shappky.R.string.no_shell_permission_error))
            }

            val appManager = com.yassernull.shappky.core.managers.BackgroundAppManager(context, handler, executor, shellManager)

            val selectUserApps = trigger.selectUserApps
            val selectSystemApps = trigger.selectSystemApps
            val selectPersistentApps = trigger.selectPersistentApps
            val excludedApps = trigger.excludedApps
            val manuallySelectedApps = trigger.manuallySelectedApps

            appManager.loadBackgroundApps { runningApps ->
              val toKill = runningApps.filter { app ->
                val matchesUser = !app.isSystemApp && !app.isPersistentApp && selectUserApps
                val matchesSystem = app.isSystemApp && selectSystemApps
                val matchesPersistent = app.isPersistentApp && selectPersistentApps
                val matchesManual = manuallySelectedApps.contains(app.packageName)
                val isExcluded = excludedApps.contains(app.packageName)

                (matchesUser || matchesSystem || matchesPersistent || matchesManual) && !isExcluded && !app.isProtected
              }

              val packageNamesToKill = toKill.map { it.packageName }
              if (packageNamesToKill.isNotEmpty()) {
                appManager.killPackages(packageNamesToKill, {
                  val totalKb = toKill.sumOf { it.ramKb }
                  val freedText = context.getString(com.yassernull.shappky.R.string.free_up_memory, appManager.formatMemorySize(totalKb))
                  Log.d(TAG, "Tasker Trigger '${trigger.name}' executed successfully. Freed: $freedText")
                }, false)
              } else {
                Log.d(TAG, "No apps matching trigger rules were running.")
              }
            }
          } else {
            Log.e(TAG, "Trigger with ID $triggerId not found in SharedPreferences!")
            return TaskerPluginResultError(1, context.getString(com.yassernull.shappky.R.string.trigger_not_found_error, triggerId))
          }
        } else {
          Log.e(TAG, "No Trigger ID provided in the Tasker configuration!")
          return TaskerPluginResultError(2, context.getString(com.yassernull.shappky.R.string.no_trigger_id_error))
        }
      }
      else -> {
        Log.w(TAG, "Unknown actionType received: $actionType")
      }
    }
    Log.d(TAG, "Tasker plugin action execution completed successfully.")
    return TaskerPluginResultSucess()
  }
}
