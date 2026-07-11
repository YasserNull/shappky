package com.yn.shappky.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.yn.shappky.R
import com.yn.shappky.data.models.TriggerModel
import com.yn.shappky.utils.BackgroundAppManager
import com.yn.shappky.utils.ShellManager
import com.yn.shappky.utils.TriggerAlarmManager
import com.yn.shappky.utils.TriggerManager
import java.util.concurrent.Executors

class AlarmReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "AlarmReceiver"
  }

  override fun onReceive(context: Context, intent: Intent) {
    val triggerId = intent.getStringExtra("trigger_id") ?: return
    Log.d(TAG, "Alarm fired for trigger ID: $triggerId")

    val triggers = TriggerManager.getTriggers(context)
    val trigger = triggers.firstOrNull { it.id == triggerId }

    if (trigger != null && trigger.isEnabled) {
      executeAlarmTrigger(context, trigger)
      // Reschedule for tomorrow
      TriggerAlarmManager.scheduleAlarmForTrigger(context, trigger)
    }
  }

  private fun executeAlarmTrigger(context: Context, trigger: TriggerModel) {
    Log.d(TAG, "executeAlarmTrigger: Executing trigger '${trigger.name}'")

    val handler = Handler(Looper.getMainLooper())
    val executor = Executors.newSingleThreadExecutor()
    val shellManager = ShellManager(context, handler, executor)
    shellManager.checkShellPermissions()

    if (!shellManager.hasAnyShellPermission()) {
      Log.w(TAG, "executeAlarmTrigger: Skipped due to lack of shell permissions")
      executor.shutdown()
      return
    }

    val appManager = BackgroundAppManager(context, handler, executor, shellManager)
    appManager.loadBackgroundApps { runningApps ->
      val selectUserApps = trigger.selectUserApps
      val selectSystemApps = trigger.selectSystemApps
      val selectPersistentApps = trigger.selectPersistentApps
      val excludedApps = trigger.excludedApps
      val manuallySelectedApps = trigger.manuallySelectedApps

      val toKill = runningApps.filter { app ->
        val matchesUser = !app.isSystemApp && !app.isPersistentApp && selectUserApps
        val matchesSystem = app.isSystemApp && selectSystemApps
        val matchesPersistent = app.isPersistentApp && selectPersistentApps
        val matchesManual = manuallySelectedApps.contains(app.packageName)
        val isExcluded = excludedApps.contains(app.packageName)

        (matchesUser || matchesSystem || matchesPersistent || matchesManual) && !isExcluded && !app.isProtected
      }

      val packageNamesToKill = toKill.map { it.packageName }
      Log.d(TAG, "executeAlarmTrigger: Target packages to kill: $packageNamesToKill")
      if (packageNamesToKill.isNotEmpty()) {
        appManager.killPackages(packageNamesToKill, {
          val totalKb = toKill.sumOf { it.ramKb }
          val freedText = context.getString(R.string.free_up_memory, appManager.formatMemorySize(totalKb))
          Log.d(TAG, "executeAlarmTrigger: Kill completed successfully. Freed memory: $freedText")
          com.yn.shappky.utils.LanguageHelper.showTriggerFreedMemoryNotification(context, trigger.name, freedText)
          executor.shutdown()
        }, showToast = false)
      } else {
        Log.d(TAG, "executeAlarmTrigger: No packages matched search filters to kill")
        executor.shutdown()
      }
    }
  }
}
