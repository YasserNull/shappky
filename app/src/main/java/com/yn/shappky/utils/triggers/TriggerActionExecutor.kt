package com.yn.shappky.utils.triggers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.util.Log
import com.yn.shappky.R
import com.yn.shappky.data.models.TriggerModel
import com.yn.shappky.data.models.TriggerRule
import com.yn.shappky.services.ShappkyService
import com.yn.shappky.utils.BackgroundAppManager
import com.yn.shappky.utils.LanguageHelper
import com.yn.shappky.utils.ProtectionManager
import com.yn.shappky.utils.ShellManager
import java.util.concurrent.ExecutorService

class TriggerActionExecutor(
  private val context: Context,
  private val handler: Handler,
  private val executor: ExecutorService,
  private val shellManager: ShellManager,
) {
  companion object {
    private const val TAG = "TriggerActionExecutor"
    private const val PREFERENCES_NAME = "AppPreferences"
    private const val KEY_HIDDEN_APPS = "hidden_apps"
  }
  fun enableShappkyService(rule: TriggerRule) {
    Log.d(TAG, "Enable Rule triggered! Starting ShappkyService.")
    val intent = Intent(context, ShappkyService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent)
    } else {
      context.startService(intent)
    }
    LanguageHelper.showTriggerFreedMemoryNotification(
      context,
      context.getString(R.string.enable_rules_title),
      context.getString(R.string.shappky_service_enabled_notification_text),
    )
  }

  fun disableShappkyService(rule: TriggerRule) {
    Log.d(TAG, "Disable Rule triggered! Stopping ShappkyService.")
    val intent = Intent(context, ShappkyService::class.java)
    context.stopService(intent)
    LanguageHelper.showTriggerFreedMemoryNotification(
      context,
      context.getString(R.string.disable_rules_title),
      context.getString(R.string.shappky_service_disabled_notification_text),
    )
  }

  fun executeServiceTrigger(trigger: TriggerModel) {
    Log.d(TAG, "executeServiceTrigger: Starting execution for '${trigger.name}'")
    if (!shellManager.hasAnyShellPermission()) {
      Log.w(TAG, "executeServiceTrigger: Skipped due to lack of shell permissions")
      return
    }

    val selectUserApps = trigger.selectUserApps
    val selectSystemApps = trigger.selectSystemApps
    val selectPersistentApps = trigger.selectPersistentApps
    val excludedApps = trigger.excludedApps
    val manuallySelectedApps = trigger.manuallySelectedApps

    val protectedApps = ProtectionManager.getProtectedApps(context)
    val hiddenApps = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      .getStringSet(KEY_HIDDEN_APPS, HashSet()) ?: HashSet()

    val appManager = BackgroundAppManager(context, handler, executor, shellManager)

    appManager.loadBackgroundApps { runningApps ->
      Log.d(TAG, "executeServiceTrigger: Loaded ${runningApps.size} running background apps")
      val toKill = runningApps.filter { app ->
        val matchesUser = !app.isSystemApp && !app.isPersistentApp && selectUserApps
        val matchesSystem = app.isSystemApp && selectSystemApps
        val matchesPersistent = app.isPersistentApp && selectPersistentApps
        val matchesManual = manuallySelectedApps.contains(app.packageName)
        val isExcluded = excludedApps.contains(app.packageName)

        (matchesUser || matchesSystem || matchesPersistent || matchesManual) && !isExcluded && !app.isProtected
      }

      val packageNamesToKill = toKill.map { it.packageName }
      Log.d(TAG, "executeServiceTrigger: Target packages to kill: $packageNamesToKill")
      if (packageNamesToKill.isNotEmpty()) {
        val shouldKillAll = selectSystemApps || selectUserApps
        appManager.killPackages(packageNamesToKill, {
          val totalKb = toKill.sumOf { it.ramKb }
          val freedText = context.getString(R.string.free_up_memory, appManager.formatMemorySize(totalKb))
          Log.d(TAG, "executeServiceTrigger: Kill completed successfully. Freed memory: $freedText")
          LanguageHelper.showTriggerFreedMemoryNotification(context, trigger.name, freedText)
        }, showToast = false, appendKillAll = shouldKillAll)
      } else {
        Log.d(TAG, "executeServiceTrigger: No packages matched search filters to kill")
      }
    }
  }
}
