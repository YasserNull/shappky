package com.yassernull.shappky.core.managers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.yassernull.shappky.services.ShappkyTriggerService

object TriggerServiceManager {
  private const val TAG = "TriggerServiceManager"

  fun updateTriggerServiceState(context: Context) {
    TriggerAlarmManager.updateAlarms(context)
    val triggers = TriggerManager.getTriggers(context)
    val enableRules = EnableTriggerManager.getEnableRules(context)
    val disableRules = DisableTriggerManager.getDisableRules(context)
    val hasActiveTriggers = triggers.any { it.isEnabled && (it.rules.isNotEmpty() || it.enableRules.isNotEmpty() || it.disableRules.isNotEmpty()) } || enableRules.isNotEmpty() || disableRules.isNotEmpty()
    Log.d(TAG, "updateTriggerServiceState: active=$hasActiveTriggers, running=${ShappkyTriggerService.isRunning()}")
    val intent = Intent(context, ShappkyTriggerService::class.java)
    if (hasActiveTriggers) {
      if (!ShappkyTriggerService.isRunning()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      }
    } else {
      if (ShappkyTriggerService.isRunning()) {
        context.stopService(intent)
      }
    }
  }
}
