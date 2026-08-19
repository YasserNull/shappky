package com.yassernull.shappky.core.domain.evaluator

import android.content.Context
import android.util.Log
import com.yassernull.shappky.R
import com.yassernull.shappky.core.domain.executors.TriggerActionExecutor
import com.yassernull.shappky.core.domain.trackers.SystemStateTracker
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.data.models.TriggerRule
import com.yassernull.shappky.services.ShappkyService
import com.yassernull.shappky.utils.NotificationUtils
import java.util.concurrent.ConcurrentHashMap

class ServiceStateRuleEvaluator(
  private val context: Context,
  private val actionExecutor: TriggerActionExecutor,
  private val stateTracker: SystemStateTracker,
  private val lastExecutedTime: ConcurrentHashMap<String, Long>,
) {
  companion object {
    private const val TAG = "ServiceStateRuleEvaluator"
    private const val SERVICE_RULE_COOLDOWN_MS = 60000L
  }

  fun evaluateServiceStateRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    isPhoneSleepTriggered: Boolean,
    isPhoneWakeTriggered: Boolean,
    usedMb: Long,
    now: Long,
  ) {
    val isShappkyServiceRunning = ShappkyService.isRunning()
    Log.d(TAG, "evaluateServiceStateRules: triggers=${triggers.size}, sleepTriggered=$isPhoneSleepTriggered, wakeTriggered=$isPhoneWakeTriggered, usedMb=$usedMb, serviceRunning=$isShappkyServiceRunning, enableRules=${enableRules.size}, disableRules=${disableRules.size}")

    // Evaluate rules for active triggers
    for (trigger in triggers) {
      for (rule in trigger.rules) {
        var isRuleTriggered = false
        var ruleCooldownMs = 60000L

        when (rule.type) {
          RuleType.SPECIFIC_TIME -> {}
          RuleType.PHONE_SLEEP -> if (isPhoneSleepTriggered) isRuleTriggered = true
          RuleType.PHONE_WAKE -> if (isPhoneWakeTriggered) isRuleTriggered = true
          RuleType.RAM_LIMIT_REACHED -> {
            if (usedMb >= rule.ramThresholdMb) {
              isRuleTriggered = true
              ruleCooldownMs = 120000L
            }
          }
          RuleType.SERVICE_STATE_CHANGED -> {
            for (serviceKey in rule.selectedServices) {
              val changed = stateTracker.hasServiceStateChanged(serviceKey)
              Log.d(TAG, "SERVICE_STATE_CHANGED check: key=$serviceKey changed=$changed")
              if (changed) {
                isRuleTriggered = true
                Log.d(TAG, "Service state changed: $serviceKey")
                break
              }
            }
          }
          else -> {}
        }

        if (isRuleTriggered) {
          val lastRun = lastExecutedTime[rule.id] ?: 0L
          if (rule.type == RuleType.SPECIFIC_TIME || (now - lastRun >= ruleCooldownMs)) {
            lastExecutedTime[rule.id] = now
            Log.d(TAG, "Rule matched! Triggering '${trigger.name}' due to rule type ${rule.type}")
            actionExecutor.executeServiceTrigger(trigger)
          }
        }
      }
    }

    // Evaluate Enable Rules and Disable Rules
    val allServiceRules = mutableListOf<TriggerRule>()
    if (!isShappkyServiceRunning) {
      allServiceRules.addAll(enableRules)
    } else {
      allServiceRules.addAll(disableRules)
    }

    for (rule in allServiceRules) {
      var isRuleTriggered = false
      var ruleCooldownMs = 60000L

      when (rule.type) {
        RuleType.SPECIFIC_TIME -> {}
        RuleType.PHONE_SLEEP -> if (isPhoneSleepTriggered) isRuleTriggered = true
        RuleType.PHONE_WAKE -> if (isPhoneWakeTriggered) isRuleTriggered = true
        RuleType.RAM_LIMIT_REACHED -> {
          if (usedMb >= rule.ramThresholdMb) {
            isRuleTriggered = true
            ruleCooldownMs = 120000L
          }
        }
        RuleType.SERVICE_STATE_CHANGED -> {
          for (serviceKey in rule.selectedServices) {
            val changed = stateTracker.hasServiceStateChanged(serviceKey)
            Log.d(TAG, "SERVICE_STATE_CHANGED check (service rule): key=$serviceKey changed=$changed")
            if (changed) {
              isRuleTriggered = true
              break
            }
          }
        }
        else -> {}
      }

      if (isRuleTriggered) {
        val lastRun = lastExecutedTime[rule.id] ?: 0L
        if (rule.type == RuleType.SPECIFIC_TIME || (now - lastRun >= ruleCooldownMs)) {
          lastExecutedTime[rule.id] = now
          Log.d(TAG, "Enable/Disable rule matched! type=${rule.type}, serviceRunning=$isShappkyServiceRunning")
          if (!isShappkyServiceRunning) {
            actionExecutor.enableShappkyService(rule)
          } else {
            actionExecutor.disableShappkyService(rule)
          }
        }
      }
    }
  }

  fun evaluateTriggerEnableDisableRules(
    triggers: List<TriggerModel>,
    isPhoneSleepTriggered: Boolean,
    isPhoneWakeTriggered: Boolean,
    usedMb: Long,
    now: Long,
  ) {
    for (trigger in triggers) {
      for (rule in trigger.enableRules) {
        if (!matchesStateRule(rule, isPhoneSleepTriggered, isPhoneWakeTriggered, usedMb)) continue
        val lastRun = lastExecutedTime[rule.id] ?: 0L
        if (now - lastRun >= SERVICE_RULE_COOLDOWN_MS) {
          lastExecutedTime[rule.id] = now
          if (!trigger.isEnabled) {
            TriggerManager.setTriggerEnabled(context, trigger.id, true)
            Log.d(TAG, "Enable rule matched! Trigger '${trigger.name}' enabled (rule ${rule.type})")
            NotificationUtils.showTriggerFreedMemoryNotification(
              context,
              trigger.name,
              context.getString(R.string.trigger_enabled_notification_text),
            )
          }
        }
      }
      for (rule in trigger.disableRules) {
        if (!matchesStateRule(rule, isPhoneSleepTriggered, isPhoneWakeTriggered, usedMb)) continue
        val lastRun = lastExecutedTime[rule.id] ?: 0L
        if (now - lastRun >= SERVICE_RULE_COOLDOWN_MS) {
          lastExecutedTime[rule.id] = now
          if (trigger.isEnabled) {
            TriggerManager.setTriggerEnabled(context, trigger.id, false)
            Log.d(TAG, "Disable rule matched! Trigger '${trigger.name}' disabled (rule ${rule.type})")
            NotificationUtils.showTriggerFreedMemoryNotification(
              context,
              trigger.name,
              context.getString(R.string.trigger_disabled_notification_text),
            )
          }
        }
      }
    }
  }

  private fun matchesStateRule(
    rule: TriggerRule,
    isPhoneSleepTriggered: Boolean,
    isPhoneWakeTriggered: Boolean,
    usedMb: Long,
  ): Boolean = when (rule.type) {
    RuleType.PHONE_SLEEP -> isPhoneSleepTriggered
    RuleType.PHONE_WAKE -> isPhoneWakeTriggered
    RuleType.RAM_LIMIT_REACHED -> usedMb >= rule.ramThresholdMb
    RuleType.SERVICE_STATE_CHANGED -> rule.selectedServices.any { stateTracker.hasServiceStateChanged(it) }
    else -> false
  }
}
