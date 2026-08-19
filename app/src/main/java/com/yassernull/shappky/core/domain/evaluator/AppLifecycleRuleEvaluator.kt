package com.yassernull.shappky.core.domain.evaluator

import android.content.Context
import android.util.Log
import com.yassernull.shappky.R
import com.yassernull.shappky.core.domain.executors.TriggerActionExecutor
import com.yassernull.shappky.core.domain.trackers.AppForegroundTracker
import com.yassernull.shappky.core.managers.KillTracker
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.data.models.TriggerRule
import com.yassernull.shappky.services.ShappkyService
import com.yassernull.shappky.utils.NotificationUtils
import java.util.concurrent.ConcurrentHashMap

class AppLifecycleRuleEvaluator(
  private val context: Context,
  private val actionExecutor: TriggerActionExecutor,
  private val foregroundTracker: AppForegroundTracker,
  private val lastExecutedTime: ConcurrentHashMap<String, Long>,
  private val recentlyReportedExits: ConcurrentHashMap<String, Long>,
) {
  companion object {
    private const val TAG = "AppLifecycleRuleEvaluator"
    private const val APP_FOREGROUND_RULE_COOLDOWN_MS = 60000L
    private const val EXIT_REPORT_COOLDOWN_MS = 20000L
  }

  fun evaluateAppForegroundRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    currentForeground: String?,
    previouslyForeground: String?,
    currentForegroundWasKnown: Boolean,
  ) {
    val isShappkyServiceRunning = ShappkyService.isRunning()
    val now = System.currentTimeMillis()
    Log.d(TAG, "evaluateAppForegroundRules: currentForeground=$currentForeground, previouslyForeground=$previouslyForeground, triggers=${triggers.size}")

    for (trigger in triggers) {
      val prev = previouslyForeground
      val current = currentForeground
      val isTransition = prev != null && current != null && prev != current

      val appOpenedRules = trigger.rules.filter { it.type == RuleType.APP_OPENED }
      if (isTransition && !currentForegroundWasKnown && appOpenedRules.any { rule -> rule.appPackages.contains(current) }) {
        val matchedRule = appOpenedRules.first { rule -> rule.appPackages.contains(current) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          Log.d(TAG, "APP_OPENED MATCH FOUND! Triggering '${trigger.name}' for $current (first time, not running before)")
          actionExecutor.executeServiceTrigger(trigger)
        }
      }

      val appResumedRules = trigger.rules.filter { it.type == RuleType.APP_RESUMED }
      if (appResumedRules.isNotEmpty()) {
        Log.d(TAG, "APP_RESUMED check: trigger='${trigger.name}', rules=${appResumedRules.size}, current=$current, wasKnown=$currentForegroundWasKnown, isTransition=$isTransition")
      }
      if (isTransition && currentForegroundWasKnown && appResumedRules.any { rule -> rule.appPackages.contains(current) }) {
        val matchedRule = appResumedRules.first { rule -> rule.appPackages.contains(current) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          Log.d(TAG, "APP_RESUMED MATCH FOUND! Triggering '${trigger.name}' for $current")
          actionExecutor.executeServiceTrigger(trigger)
        }
      }

      val appPausedRules = trigger.rules.filter { it.type == RuleType.APP_PAUSED }
      if (appPausedRules.isNotEmpty()) {
        Log.d(TAG, "APP_PAUSED check: trigger='${trigger.name}', rules=${appPausedRules.size}, previous=$prev, isTransition=$isTransition")
      }
      if (prev != null && isTransition && foregroundTracker.isKnownPackage(prev) && appPausedRules.any { rule -> rule.appPackages.contains(prev) }) {
        val matchedRule = appPausedRules.first { rule -> rule.appPackages.contains(prev) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          Log.d(TAG, "APP_PAUSED MATCH FOUND! Triggering '${trigger.name}' for $prev")
          actionExecutor.executeServiceTrigger(trigger)
        }
      }
    }

    if (!isShappkyServiceRunning && enableRules.isNotEmpty()) {
      val prev = previouslyForeground
      val current = currentForeground
      val isTransition = prev != null && current != null && prev != current

      val appOpenedRules = enableRules.filter { it.type == RuleType.APP_OPENED }
      if (isTransition && !currentForegroundWasKnown && appOpenedRules.any { rule -> rule.appPackages.contains(current) }) {
        val matchedRule = appOpenedRules.first { rule -> rule.appPackages.contains(current) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          actionExecutor.enableShappkyService(matchedRule)
        }
      }

      val appResumedRules = enableRules.filter { it.type == RuleType.APP_RESUMED }
      if (isTransition && currentForegroundWasKnown && appResumedRules.any { rule -> rule.appPackages.contains(current) }) {
        val matchedRule = appResumedRules.first { rule -> rule.appPackages.contains(current) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          actionExecutor.enableShappkyService(matchedRule)
        }
      }

      val appPausedRules = enableRules.filter { it.type == RuleType.APP_PAUSED }
      if (prev != null && isTransition && foregroundTracker.isKnownPackage(prev) && appPausedRules.any { rule -> rule.appPackages.contains(prev) }) {
        actionExecutor.enableShappkyService(appPausedRules.first())
      }
    }

    if (isShappkyServiceRunning && disableRules.isNotEmpty()) {
      val prev = previouslyForeground
      val current = currentForeground
      val isTransition = prev != null && current != null && prev != current

      val appOpenedRules = disableRules.filter { it.type == RuleType.APP_OPENED }
      if (isTransition && !currentForegroundWasKnown && appOpenedRules.any { rule -> rule.appPackages.contains(current) }) {
        val matchedRule = appOpenedRules.first { rule -> rule.appPackages.contains(current) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          actionExecutor.disableShappkyService(matchedRule)
        }
      }

      val appResumedRules = disableRules.filter { it.type == RuleType.APP_RESUMED }
      if (isTransition && currentForegroundWasKnown && appResumedRules.any { rule -> rule.appPackages.contains(current) }) {
        val matchedRule = appResumedRules.first { rule -> rule.appPackages.contains(current) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          actionExecutor.disableShappkyService(matchedRule)
        }
      }

      val appPausedRules = disableRules.filter { it.type == RuleType.APP_PAUSED }
      if (prev != null && isTransition && foregroundTracker.isKnownPackage(prev) && appPausedRules.any { rule -> rule.appPackages.contains(prev) }) {
        actionExecutor.disableShappkyService(appPausedRules.first())
      }
    }
  }

  fun handleSleepAppPausedRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    prevApp: String,
  ) {
    val isShappkyServiceRunning = ShappkyService.isRunning()
    Log.d(TAG, "handleSleepAppPausedRules: prevApp=$prevApp, serviceRunning=$isShappkyServiceRunning")

    for (trigger in triggers) {
      val appPausedRules = trigger.rules.filter { it.type == RuleType.APP_PAUSED }
      Log.d(TAG, "APP_PAUSED check (sleep): trigger='${trigger.name}', rules=${appPausedRules.size}, previous=$prevApp")
      if (appPausedRules.any { rule -> rule.appPackages.contains(prevApp) }) {
        Log.d(TAG, "APP_PAUSED MATCH FOUND (Sleep)! Triggering '${trigger.name}' for $prevApp")
        actionExecutor.executeServiceTrigger(trigger)
      }
    }
    if (!isShappkyServiceRunning && enableRules.isNotEmpty()) {
      val appPausedRules = enableRules.filter { it.type == RuleType.APP_PAUSED }
      if (appPausedRules.any { rule -> rule.appPackages.contains(prevApp) }) {
        actionExecutor.enableShappkyService(appPausedRules.first())
      }
    }
    if (isShappkyServiceRunning && disableRules.isNotEmpty()) {
      val appPausedRules = disableRules.filter { it.type == RuleType.APP_PAUSED }
      if (appPausedRules.any { rule -> rule.appPackages.contains(prevApp) }) {
        actionExecutor.disableShappkyService(appPausedRules.first())
      }
    }
  }

  fun evaluateAppExitRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    killedPackages: Set<String>,
    swipedFromRecentsPackages: Set<String>,
  ) {
    val isShappkyServiceRunning = ShappkyService.isRunning()
    val stoppedSet = killedPackages + swipedFromRecentsPackages

    Log.d(TAG, "evaluateAppExitRules: processStopped=$killedPackages, swipedFromRecents=$swipedFromRecentsPackages, serviceRunning=$isShappkyServiceRunning")

    for (pkg in stoppedSet) {
      val now = System.currentTimeMillis()
      val killedByShappky = KillTracker.contains(pkg)
      if (!killedByShappky) {
        val lastReported = recentlyReportedExits[pkg] ?: 0L
        if (now - lastReported < EXIT_REPORT_COOLDOWN_MS) {
          Log.d(TAG, "evaluateAppExitRules: $pkg already reported recently, skipping")
          continue
        }
        recentlyReportedExits[pkg] = now
      }
      Log.d(TAG, "evaluateAppExitRules: pkg=$pkg, killedByShappky=$killedByShappky")
      for (trigger in triggers) {
        if (killedByShappky) {
          val killedRules = trigger.rules.filter { it.type == RuleType.APP_KILLED }
          if (killedRules.any { rule -> rule.appPackages.contains(pkg) }) {
            Log.d(TAG, "APP_KILLED MATCH FOUND! Triggering '${trigger.name}' for $pkg")
            actionExecutor.executeServiceTrigger(trigger)
          }
        } else {
          val exitedRules = trigger.rules.filter { it.type == RuleType.APP_EXITED }
          if (exitedRules.any { rule -> rule.appPackages.contains(pkg) }) {
            Log.d(TAG, "APP_EXITED MATCH FOUND! Triggering '${trigger.name}' for $pkg")
            actionExecutor.executeServiceTrigger(trigger)
          }
        }
      }
      if (!isShappkyServiceRunning && enableRules.isNotEmpty()) {
        if (killedByShappky) {
          val killedRules = enableRules.filter { it.type == RuleType.APP_KILLED }
          if (killedRules.any { rule -> rule.appPackages.contains(pkg) }) {
            actionExecutor.enableShappkyService(killedRules.first())
          }
        } else {
          val exitedRules = enableRules.filter { it.type == RuleType.APP_EXITED }
          if (exitedRules.any { rule -> rule.appPackages.contains(pkg) }) {
            actionExecutor.enableShappkyService(exitedRules.first())
          }
        }
      }
      if (isShappkyServiceRunning && disableRules.isNotEmpty()) {
        if (killedByShappky) {
          val killedRules = disableRules.filter { it.type == RuleType.APP_KILLED }
          if (killedRules.any { rule -> rule.appPackages.contains(pkg) }) {
            actionExecutor.disableShappkyService(killedRules.first())
          }
        } else {
          val exitedRules = disableRules.filter { it.type == RuleType.APP_EXITED }
          if (exitedRules.any { rule -> rule.appPackages.contains(pkg) }) {
            actionExecutor.disableShappkyService(exitedRules.first())
          }
        }
      }
    }
  }

  fun evaluateTriggerEnableDisableAppRules(
    triggers: List<TriggerModel>,
    currentForeground: String?,
    previouslyForeground: String?,
    currentForegroundWasKnown: Boolean,
    stoppedPackages: Set<String>,
    now: Long,
  ) {
    for (trigger in triggers) {
      for (rule in trigger.enableRules) {
        if (!matchesAppRule(rule, currentForeground, previouslyForeground, currentForegroundWasKnown, stoppedPackages)) continue
        val lastRun = lastExecutedTime[rule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[rule.id] = now
          if (!trigger.isEnabled) {
            TriggerManager.setTriggerEnabled(context, trigger.id, true)
            Log.d(TAG, "Enable rule matched (app)! Trigger '${trigger.name}' enabled (rule ${rule.type})")
            NotificationUtils.showTriggerFreedMemoryNotification(
              context,
              trigger.name,
              context.getString(R.string.trigger_enabled_notification_text),
            )
          }
        }
      }
      for (rule in trigger.disableRules) {
        if (!matchesAppRule(rule, currentForeground, previouslyForeground, currentForegroundWasKnown, stoppedPackages)) continue
        val lastRun = lastExecutedTime[rule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[rule.id] = now
          if (trigger.isEnabled) {
            TriggerManager.setTriggerEnabled(context, trigger.id, false)
            Log.d(TAG, "Disable rule matched (app)! Trigger '${trigger.name}' disabled (rule ${rule.type})")
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

  private fun matchesAppRule(
    rule: TriggerRule,
    currentForeground: String?,
    previouslyForeground: String?,
    currentForegroundWasKnown: Boolean,
    stoppedPackages: Set<String>,
  ): Boolean {
    val isTransition = previouslyForeground != null && currentForeground != null && previouslyForeground != currentForeground
    return when (rule.type) {
      RuleType.APP_OPENED -> isTransition && !currentForegroundWasKnown && rule.appPackages.contains(currentForeground)
      RuleType.APP_RESUMED -> isTransition && currentForegroundWasKnown && rule.appPackages.contains(currentForeground)
      RuleType.APP_PAUSED -> isTransition && rule.appPackages.contains(previouslyForeground)
      RuleType.APP_EXITED -> rule.appPackages.any { it in stoppedPackages && !KillTracker.contains(it) }
      RuleType.APP_KILLED -> rule.appPackages.any { it in stoppedPackages && KillTracker.contains(it) }
      else -> false
    }
  }

  fun cleanKilledApps(runningPackages: Set<String>) {
    KillTracker.cleanUp(runningPackages)
    val now = System.currentTimeMillis()
    val iteratorReported = recentlyReportedExits.entries.iterator()
    while (iteratorReported.hasNext()) {
      val entry = iteratorReported.next()
      if (now - entry.value >= EXIT_REPORT_COOLDOWN_MS) {
        iteratorReported.remove()
      }
    }
  }
}
