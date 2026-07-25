package com.yassernull.shappky.core.managers

import android.content.Context
import android.content.Intent
import android.os.Build
import com.yassernull.shappky.services.ShappkyTriggerService

object TriggerServiceManager {
  fun updateTriggerServiceState(context: Context) {
    TriggerAlarmManager.updateAlarms(context)
    val triggers = TriggerManager.getTriggers(context)
    val enableRules = EnableTriggerManager.getEnableRules(context)
    val hasActiveTriggers = triggers.any { it.isEnabled && it.rules.isNotEmpty() } || enableRules.isNotEmpty()
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
