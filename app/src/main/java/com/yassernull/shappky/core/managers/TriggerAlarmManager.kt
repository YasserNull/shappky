package com.yassernull.shappky.core.managers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.receivers.AlarmReceiver
import java.util.Calendar

object TriggerAlarmManager {
  private const val TAG = "TriggerAlarmManager"
  const val ACTION_RULE_ALARM = "com.yassernull.shappky.ACTION_RULE_ALARM"
  const val EXTRA_RULE_ID = "rule_id"
  const val EXTRA_IS_ENABLE_RULE = "is_enable_rule"

  fun updateAlarms(context: Context) {
    cancelAllAlarms(context)
    val triggers = TriggerManager.getTriggers(context)
    val activeTimeTriggers = triggers.filter {
      it.isEnabled && it.rules.any { rule -> rule.type == RuleType.SPECIFIC_TIME }
    }
    for (trigger in activeTimeTriggers) {
      scheduleAlarmForTrigger(context, trigger)
    }
    val enableRules = EnableTriggerManager.getEnableRules(context)
    val disableRules = DisableTriggerManager.getDisableRules(context)
    for (rule in enableRules) {
      if (rule.type == RuleType.SPECIFIC_TIME) {
        scheduleAlarmForRule(context, rule, isEnableRule = true)
      }
    }
    for (rule in disableRules) {
      if (rule.type == RuleType.SPECIFIC_TIME) {
        scheduleAlarmForRule(context, rule, isEnableRule = false)
      }
    }
  }

  fun scheduleAlarmForRule(context: Context, rule: com.yassernull.shappky.data.models.TriggerRule, isEnableRule: Boolean) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val calendar = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, rule.timeHour)
      set(Calendar.MINUTE, rule.timeMinute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = ACTION_RULE_ALARM
      putExtra(EXTRA_RULE_ID, rule.id)
      putExtra(EXTRA_IS_ENABLE_RULE, isEnableRule)
      setData(Uri.parse("custom://" + (if (isEnableRule) "enable-" else "disable-") + rule.id))
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      rule.id.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent,
        )
      } else {
        alarmManager.setExact(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent,
        )
      }
      Log.d(TAG, "Scheduled ${if (isEnableRule) "enable" else "disable"} rule alarm at ${calendar.time}")
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException scheduling exact alarm, falling back to inexact", e)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent,
        )
      } else {
        alarmManager.set(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent,
        )
      }
    }
  }

  fun scheduleAlarmForTrigger(context: Context, trigger: TriggerModel) {
    val timeRule = trigger.rules.firstOrNull { it.type == RuleType.SPECIFIC_TIME } ?: return
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val calendar = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, timeRule.timeHour)
      set(Calendar.MINUTE, timeRule.timeMinute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = "com.yassernull.shappky.ACTION_TRIGGER_ALARM"
      putExtra("trigger_id", trigger.id)
      setData(Uri.parse("custom://" + trigger.id))
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      trigger.id.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent,
        )
      } else {
        alarmManager.setExact(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent,
        )
      }
      Log.d(TAG, "Scheduled alarm for trigger '${trigger.name}' at ${calendar.time}")
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException scheduling exact alarm, falling back to inexact", e)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent,
        )
      } else {
        alarmManager.set(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent,
        )
      }
    }
  }

  fun cancelAllAlarms(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val triggers = TriggerManager.getTriggers(context)
    for (trigger in triggers) {
      val intent = Intent(context, AlarmReceiver::class.java).apply {
        action = "com.yassernull.shappky.ACTION_TRIGGER_ALARM"
        setData(Uri.parse("custom://" + trigger.id))
      }
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        trigger.id.hashCode(),
        intent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
      )
      if (pendingIntent != null) {
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
      }
    }
    val enableRules = EnableTriggerManager.getEnableRules(context)
    val disableRules = DisableTriggerManager.getDisableRules(context)
    for (rule in enableRules) {
      cancelRuleAlarm(context, alarmManager, rule, isEnableRule = true)
    }
    for (rule in disableRules) {
      cancelRuleAlarm(context, alarmManager, rule, isEnableRule = false)
    }
    Log.d(TAG, "Cancelled all scheduled alarms")
  }

  private fun cancelRuleAlarm(context: Context, alarmManager: AlarmManager, rule: com.yassernull.shappky.data.models.TriggerRule, isEnableRule: Boolean) {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
      action = ACTION_RULE_ALARM
      setData(Uri.parse("custom://" + (if (isEnableRule) "enable-" else "disable-") + rule.id))
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      rule.id.hashCode(),
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )
    if (pendingIntent != null) {
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }
}
