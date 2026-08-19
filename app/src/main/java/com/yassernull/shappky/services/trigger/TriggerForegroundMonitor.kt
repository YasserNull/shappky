package com.yassernull.shappky.services.trigger

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.util.Log
import com.yassernull.shappky.core.domain.evaluator.TriggerRuleEvaluator
import com.yassernull.shappky.core.domain.trackers.AppForegroundTracker
import com.yassernull.shappky.core.domain.trackers.SystemStateTracker
import com.yassernull.shappky.core.managers.DisableTriggerManager
import com.yassernull.shappky.core.managers.EnableTriggerManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.services.ShappkyService
import java.util.concurrent.ExecutorService

class TriggerForegroundMonitor(
  private val context: Context,
  private val handler: Handler,
  private val shellManager: ShellManager,
  private val stateTracker: SystemStateTracker,
  private val foregroundTracker: AppForegroundTracker,
  private val ruleEvaluator: TriggerRuleEvaluator,
  private val executor: ExecutorService,
  private val sharedState: TriggerMonitoringState,
  private val isRunning: () -> Boolean,
) {
  companion object {
    private const val TAG = "TriggerForegroundMonitor"
    private const val MONITOR_SCAN_INTERVAL_MS = 1000L
    private const val MONITOR_COMMAND_TIMEOUT_MS = 10000L
    private const val EXIT_CONFIRM_TIMEOUT_MS = 5000L
  }

  private val pendingExitChecks = mutableMapOf<String, Long>()

  fun start() {
    stateTracker.initializeStates()

    executor.execute {
      while (isRunning()) {
        try {
          val triggers = TriggerManager.getTriggers(context)
          val activeTriggers = triggers.filter { it.isEnabled }
          val enableRules = EnableTriggerManager.getEnableRules(context)
          val disableRules = DisableTriggerManager.getDisableRules(context)

          val isShappkyServiceRunning = ShappkyService.isRunning()
          val hasTriggerToggleRules = triggers.any { it.enableRules.isNotEmpty() || it.disableRules.isNotEmpty() }
          val hasWorkToDo = activeTriggers.isNotEmpty() ||
            hasTriggerToggleRules ||
            (!isShappkyServiceRunning && enableRules.isNotEmpty()) ||
            (isShappkyServiceRunning && disableRules.isNotEmpty())

          if (!hasWorkToDo) {
            Thread.sleep(10000L)
            continue
          }

          val scanIntervalMs = MONITOR_SCAN_INTERVAL_MS
          Log.d(TAG, "startTriggerMonitoring: scanning every ${scanIntervalMs}ms")

          val now = System.currentTimeMillis()

          val oldInteractive = stateTracker.currentInteractive
          stateTracker.updateCurrentStates()

          var isPhoneSleepTriggered = false
          var isPhoneWakeTriggered = false

          if (stateTracker.lastInteractiveState != null && stateTracker.lastInteractiveState != stateTracker.currentInteractive) {
            if (!stateTracker.currentInteractive) {
              isPhoneSleepTriggered = true
            } else {
              isPhoneWakeTriggered = true
            }
          }

          if (sharedState.pendingSleepEvent) {
            sharedState.pendingSleepEvent = false
            isPhoneSleepTriggered = true
          }
          if (sharedState.pendingWakeEvent) {
            sharedState.pendingWakeEvent = false
            isPhoneWakeTriggered = true
          }
          if (isPhoneSleepTriggered || isPhoneWakeTriggered) {
            Log.d(TAG, "startTriggerMonitoring: sleepEvent=$isPhoneSleepTriggered, wakeEvent=$isPhoneWakeTriggered")
          }

          val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
          val memoryInfo = ActivityManager.MemoryInfo()
          activityManager?.getMemoryInfo(memoryInfo)
          val totalMb = memoryInfo.totalMem / (1024 * 1024)
          val availMb = memoryInfo.availMem / (1024 * 1024)
          val usedMb = totalMb - availMb

          // 1. Evaluate Service States and General Rules
          ruleEvaluator.evaluateTriggerEnableDisableRules(
            triggers,
            isPhoneSleepTriggered,
            isPhoneWakeTriggered,
            usedMb,
            now,
          )

          ruleEvaluator.evaluateServiceStateRules(
            activeTriggers,
            enableRules,
            disableRules,
            isPhoneSleepTriggered,
            isPhoneWakeTriggered,
            usedMb,
            now,
          )

          stateTracker.saveCurrentStatesAsLast()

          // 2. Evaluate Foreground Apps
          val triggerToggleRules = triggers.flatMap { it.enableRules + it.disableRules }
          val hasInactivityRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_INACTIVITY } } || triggerToggleRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_INACTIVITY } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_INACTIVITY })
          val hasAutoBgRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_BACKGROUND_STARTED } }
          val hasAppOpenedRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_OPENED } } || triggerToggleRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_OPENED } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_OPENED })
          val hasAppResumedRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_RESUMED } } || triggerToggleRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_RESUMED } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_RESUMED }) || (isShappkyServiceRunning && disableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_RESUMED })
          val hasAppPausedRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_PAUSED } } || triggerToggleRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_PAUSED } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_PAUSED }) || (isShappkyServiceRunning && disableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_PAUSED })
          val hasExitRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_EXITED } } || triggerToggleRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_EXITED } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_EXITED }) || (isShappkyServiceRunning && disableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_EXITED })
          val hasKillRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_KILLED } } || triggerToggleRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_KILLED } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_KILLED }) || (isShappkyServiceRunning && disableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_KILLED })

          Log.d(TAG, "startTriggerMonitoring: triggers=${activeTriggers.size}, interactive=${stateTracker.currentInteractive}, appOpened=$hasAppOpenedRules, appResumed=$hasAppResumedRules, appPaused=$hasAppPausedRules, exit=$hasExitRules, kill=$hasKillRules, inactivity=$hasInactivityRules, autoBg=$hasAutoBgRules")

          var currentForeground: String? = null
          if (stateTracker.currentInteractive && (hasAppOpenedRules || hasAppResumedRules || hasAppPausedRules || hasInactivityRules || hasAutoBgRules)) {
            if (shellManager.isShellCommandReady()) {
              val dumpOutput = shellManager.runShellCommandAndGetFullOutputWithTimeout("dumpsys activity activities", MONITOR_COMMAND_TIMEOUT_MS)
              if (dumpOutput != null) {
                currentForeground = foregroundTracker.getForegroundPackage(dumpOutput)
                val currentForegroundWasKnown = currentForeground != null && foregroundTracker.isKnownPackage(currentForeground)
                val previouslyForeground = foregroundTracker.updateForegroundApp(currentForeground, now)

                ruleEvaluator.evaluateAppForegroundRules(
                  activeTriggers,
                  enableRules,
                  disableRules,
                  currentForeground,
                  previouslyForeground,
                  currentForegroundWasKnown,
                )

                ruleEvaluator.evaluateTriggerEnableDisableAppRules(
                  triggers,
                  currentForeground,
                  previouslyForeground,
                  currentForegroundWasKnown,
                  emptySet(),
                  now,
                )

                // Fast exit detection: when the user leaves a known foreground app,
                // watch whether its activity/task disappears from the activity dump
                // (back-exit destroys the task; home-pause keeps it) and confirm exit.
                val leftApp = previouslyForeground
                if (leftApp != null && leftApp != currentForeground && (hasExitRules || hasKillRules) && foregroundTracker.isKnownPackage(leftApp)) {
                  if (!pendingExitChecks.containsKey(leftApp)) {
                    Log.d(TAG, "startTriggerMonitoring: watching $leftApp for exit (left foreground)")
                  }
                  pendingExitChecks.putIfAbsent(leftApp, now)
                }

                val pendingIterator = pendingExitChecks.entries.iterator()
                while (pendingIterator.hasNext()) {
                  val entry = pendingIterator.next()
                  val pkg = entry.key
                  val startedWatching = entry.value
                  val isShappkyKill = com.yassernull.shappky.core.managers.KillTracker.contains(pkg)
                  if (isShappkyKill || !activityDumpContainsPackage(dumpOutput, pkg)) {
                    pendingIterator.remove()
                    Log.d(TAG, "startTriggerMonitoring: exit/kill detected for $pkg (killRecorded=$isShappkyKill, no activity/task record)")
                    ruleEvaluator.evaluateAppExitRules(activeTriggers, enableRules, disableRules, emptySet(), setOf(pkg))
                  } else if (now - startedWatching > EXIT_CONFIRM_TIMEOUT_MS) {
                    pendingIterator.remove()
                    Log.d(TAG, "startTriggerMonitoring: $pkg still active after ${now - startedWatching}ms (paused, not exited)")
                  }
                }
              }
            }
          }
          sharedState.lastSharedForeground = currentForeground

          if (isPhoneSleepTriggered) {
            foregroundTracker.lastForegroundApp?.let { prevApp ->
              foregroundTracker.markAppAsInactive(prevApp, now)
              ruleEvaluator.handleSleepAppPausedRules(activeTriggers, enableRules, disableRules, prevApp)
            }
            foregroundTracker.lastForegroundApp = null
          }

          // 3. Background / RAM / Inactivity / exit & kill are evaluated by startBackgroundMonitoring()
          //    at an interval driven by the triggers' service duration.

          Thread.sleep(scanIntervalMs)
        } catch (_: InterruptedException) {
          Log.d(TAG, "startTriggerMonitoring: Monitoring loop interrupted")
          if (isRunning()) {
            Log.d(TAG, "startTriggerMonitoring: Service still running, restarting scan")
          } else {
            Thread.currentThread().interrupt()
            break
          }
        } catch (e: Throwable) {
          Log.e(TAG, "startTriggerMonitoring: Error in monitoring loop", e)
          try {
            Thread.sleep(5000L)
          } catch (_: Exception) {}
        }
      }
    }
  }

  private fun activityDumpContainsPackage(dump: String, pkg: String): Boolean {
    val escaped = Regex.escape(pkg)
    val componentRegex = Regex("$escaped(/[A-Za-z0-9_.$]*)?[\\s,}]")
    val affinityRegex = Regex("A=$escaped\\s")
    return componentRegex.containsMatchIn(dump) || affinityRegex.containsMatchIn(dump)
  }
}
