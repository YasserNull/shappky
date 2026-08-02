package com.yassernull.shappky.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.BackgroundAppManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.managers.TriggerManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ShappkyIntentReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "ShappkyIntentReceiver"
    const val ACTION_EXECUTE_TRIGGER = "com.yassernull.shappky.EXECUTE_TRIGGER"
    const val ACTION_ENABLE_SERVICE = "com.yassernull.shappky.ENABLE_SERVICE"
    const val ACTION_DISABLE_SERVICE = "com.yassernull.shappky.DISABLE_SERVICE"
    const val EXTRA_TRIGGER_ID = "TRIGGER_ID"
    const val EXTRA_TRIGGER_NAME = "TRIGGER_NAME"
  }

  override fun onReceive(context: Context, intent: Intent?) {
    Log.d(TAG, "onReceive triggered with action: ${intent?.action}")
    val pendingResult = goAsync()
    try {
      when (intent?.action) {
        ACTION_EXECUTE_TRIGGER -> handleExecuteTrigger(context, intent) {
          pendingResult.finish()
        }
        ACTION_ENABLE_SERVICE -> {
          handleEnableService(context)
          pendingResult.finish()
        }
        ACTION_DISABLE_SERVICE -> {
          handleDisableService(context)
          pendingResult.finish()
        }
        else -> pendingResult.finish()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error handling intent", e)
      pendingResult.finish()
    }
  }

  private fun handleEnableService(context: Context) {
    val serviceIntent = Intent(context, com.yassernull.shappky.services.ShappkyService::class.java)
    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
    Log.d(TAG, "Shappky Service enabled via Intent")
  }

  private fun handleDisableService(context: Context) {
    val serviceIntent = Intent(context, com.yassernull.shappky.services.ShappkyService::class.java)
    context.stopService(serviceIntent)
    Log.d(TAG, "Shappky Service disabled via Intent")
  }

  private fun handleExecuteTrigger(context: Context, intent: Intent, onDone: () -> Unit) {
    val triggerId = intent.getStringExtra(EXTRA_TRIGGER_ID)
    val triggerName = intent.getStringExtra(EXTRA_TRIGGER_NAME)

    if (triggerId == null && triggerName == null) {
      Log.e(TAG, "No TRIGGER_ID or TRIGGER_NAME provided in intent extras")
      onDone()
      return
    }

    val triggers = TriggerManager.getTriggers(context)
    val trigger = triggers.find { it.id == triggerId || (triggerName != null && it.name.equals(triggerName, ignoreCase = true)) }

    if (trigger == null) {
      Log.e(TAG, "Trigger not found! ID: $triggerId, Name: $triggerName")
      onDone()
      return
    }

    Log.d(TAG, "Trigger found: ${trigger.name}. Executing from Intent...")
    val handler = Handler(Looper.getMainLooper())
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
    val shellManager = ShellManager(context, handler, executor)

    if (!shellManager.hasAnyShellPermission()) {
      Log.e(TAG, "Cannot execute trigger: No shell permissions")
      handler.post { executor.shutdown() }
      onDone()
      return
    }

    val appManager = BackgroundAppManager(context, handler, executor, shellManager)

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
          val freedText = context.getString(R.string.free_up_memory, appManager.formatMemorySize(totalKb))
          Log.d(TAG, "Intent Trigger '${trigger.name}' executed successfully. Freed: $freedText")
          handler.post { executor.shutdown() }
          onDone()
        }, false)
      } else {
        Log.d(TAG, "No apps matching trigger rules were running.")
        handler.post { executor.shutdown() }
        onDone()
      }
    }
  }
}
