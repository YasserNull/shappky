package com.yassernull.shappky.services.trigger

import android.content.Context
import android.os.Handler
import android.util.Log
import com.yassernull.shappky.core.domain.evaluator.TriggerRuleEvaluator
import com.yassernull.shappky.core.domain.trackers.AppForegroundTracker
import com.yassernull.shappky.core.managers.DisableTriggerManager
import com.yassernull.shappky.core.managers.EnableTriggerManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.core.managers.aggregatePsOutputToPackages
import com.yassernull.shappky.core.managers.parseRecentsPackages
import com.yassernull.shappky.core.managers.psAllProcessesCommand
import com.yassernull.shappky.services.ShappkyService
import java.util.concurrent.ExecutorService

class TriggerBackgroundMonitor(
  private val context: Context,
  private val handler: Handler,
  private val shellManager: ShellManager,
  private val foregroundTracker: AppForegroundTracker,
  private val ruleEvaluator: TriggerRuleEvaluator,
  private val executor: ExecutorService,
  private val sharedState: TriggerMonitoringState,
  private val isRunning: () -> Boolean,
) {
  companion object {
    private const val TAG = "TriggerBackgroundMonitor"
    private const val DEFAULT_BACKGROUND_INTERVAL_MS = 50000L
    private const val MONITOR_COMMAND_TIMEOUT_MS = 10000L
    private const val EXIT_SCAN_INTERVAL_MS = 10000L
  }

  private val previousRunningPackages = mutableSetOf<String>()
  private val previousRecentsPackages = mutableSetOf<String>()

  fun start() {
    executor.execute {
      while (isRunning()) {
        try {
          val triggers = TriggerManager.getTriggers(context)
          val activeTriggers = triggers.filter { it.isEnabled }
          val enableRules = EnableTriggerManager.getEnableRules(context)
          val disableRules = DisableTriggerManager.getDisableRules(context)

          val isShappkyServiceRunning = ShappkyService.isRunning()
          val triggerToggleRules = triggers.flatMap { it.enableRules + it.disableRules }
          val hasInactivityRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_INACTIVITY } } || triggerToggleRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_INACTIVITY } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_INACTIVITY })
          val hasAutoBgRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_BACKGROUND_STARTED } }
          val hasKillOldestRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.KILL_OLDEST_APP } }
          val hasManualKillRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_KILLED } } || triggerToggleRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_KILLED } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_KILLED }) || (isShappkyServiceRunning && disableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_KILLED })
          val hasExitRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_EXITED } } || triggerToggleRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_EXITED } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_EXITED }) || (isShappkyServiceRunning && disableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_EXITED })
          val hasRamExceededRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_RAM_EXCEEDED } } || triggerToggleRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_RAM_EXCEEDED } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_RAM_EXCEEDED }) || (isShappkyServiceRunning && disableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_RAM_EXCEEDED })

          Log.d(TAG, "startBackgroundMonitoring: triggers=${activeTriggers.size}, inactivity=$hasInactivityRules, autoBg=$hasAutoBgRules, killOldest=$hasKillOldestRules, manualKill=$hasManualKillRules, exit=$hasExitRules, ramExceeded=$hasRamExceededRules")

          if (!hasInactivityRules && !hasAutoBgRules && !hasKillOldestRules && !hasManualKillRules && !hasExitRules && !hasRamExceededRules) {
            Thread.sleep(DEFAULT_BACKGROUND_INTERVAL_MS)
            continue
          }

          // Effective scan interval = smallest service duration among active triggers (fallback default)
          val intervalMs = minServiceDurationOrNull(activeTriggers) ?: DEFAULT_BACKGROUND_INTERVAL_MS
          val effectiveIntervalMs = if (hasExitRules || hasManualKillRules) {
            minOf(intervalMs, EXIT_SCAN_INTERVAL_MS)
          } else {
            intervalMs
          }
          Log.d(TAG, "startBackgroundMonitoring: scan in ${effectiveIntervalMs}ms (inactivity=$hasInactivityRules, autoBg=$hasAutoBgRules, killOldest=$hasKillOldestRules)")

          Thread.sleep(effectiveIntervalMs)
          if (!isRunning()) break

          val now = System.currentTimeMillis()
          if (shellManager.isShellCommandReady()) {
            val psOutput = shellManager.runShellCommandAndGetFullOutputWithTimeout(psAllProcessesCommand(), MONITOR_COMMAND_TIMEOUT_MS)
            if (psOutput != null) {
              val packageUsages = aggregatePsOutputToPackages(psOutput, context.packageManager)
              val runningPackages = packageUsages.keys.toMutableSet()
              val packageRamUsage = packageUsages.mapValues { it.value.ramKb }
              val currentForeground = sharedState.lastSharedForeground
              Log.d(TAG, "startBackgroundMonitoring: running=${runningPackages.size}, previous=${previousRunningPackages.size}, foreground=$currentForeground, added=${runningPackages - previousRunningPackages}")

              foregroundTracker.cleanUpOldForegroundRecords(runningPackages, currentForeground)
              foregroundTracker.initNewRunningPackages(runningPackages, currentForeground, now)

              // Detect apps the user removed from recents (swipe-kill), even if the process lingers in ps
              var currentRecentsPackages = emptySet<String>()
              var swipedFromRecents = emptySet<String>()
              if (hasManualKillRules || hasExitRules || hasAutoBgRules) {
                val recentsOutput = shellManager.runShellCommandAndGetFullOutputWithTimeout("dumpsys activity recents", MONITOR_COMMAND_TIMEOUT_MS) ?: ""
                if (recentsOutput.isBlank() || recentsOutput.startsWith("ERROR")) {
                  Log.w(TAG, "startBackgroundMonitoring: dumpsys activity recents empty/error, skipping recents diff")
                } else {
                  currentRecentsPackages = parseRecentsPackages(recentsOutput, context.packageManager)
                  swipedFromRecents = previousRecentsPackages - currentRecentsPackages
                  Log.d(TAG, "startBackgroundMonitoring: recents now=${currentRecentsPackages.size}, swiped=$swipedFromRecents")
                  previousRecentsPackages.clear()
                  previousRecentsPackages.addAll(currentRecentsPackages)
                }
              }

              // Check APP_EXITED / APP_KILLED (stopped processes, classified by Shappky kill)
              val killedPackages = if (previousRunningPackages.isNotEmpty()) previousRunningPackages - runningPackages else emptySet()
              val trackerKilledNotRunning = com.yassernull.shappky.core.managers.KillTracker.getKilledPackages().filter { !runningPackages.contains(it) }
              val allStopped = killedPackages + trackerKilledNotRunning
              if (allStopped.isNotEmpty() || swipedFromRecents.isNotEmpty()) {
                Log.d(TAG, "startBackgroundMonitoring: stopped=killedDiff=$killedPackages, trackerNotRunning=$trackerKilledNotRunning, swiped=$swipedFromRecents")
                ruleEvaluator.evaluateAppExitRules(activeTriggers, enableRules, disableRules, allStopped, swipedFromRecents)
                ruleEvaluator.evaluateTriggerEnableDisableAppRules(
                  triggers,
                  currentForeground,
                  foregroundTracker.lastForegroundApp,
                  false,
                  allStopped,
                  now,
                )
              }
              ruleEvaluator.cleanKilledApps(runningPackages)

              // Check APP_BACKGROUND_STARTED (running apps the user never opened - not present in recents)
              if (hasAutoBgRules) {
                ruleEvaluator.evaluateAutoStartedBackgroundRules(
                  activeTriggers,
                  runningPackages,
                  packageRamUsage,
                  currentRecentsPackages,
                )
              }
              previousRunningPackages.clear()
              previousRunningPackages.addAll(runningPackages)

              // Process APP_RAM_EXCEEDED
              ruleEvaluator.evaluateRamExceededRules(activeTriggers, enableRules, disableRules, packageRamUsage)

              // Process Inactivity / KILL_OLDEST_APP rules
              ruleEvaluator.evaluateInactivityRules(
                activeTriggers,
                enableRules,
                disableRules,
                runningPackages,
                currentForeground,
                packageRamUsage,
                now,
              )
            }
          }
        } catch (_: InterruptedException) {
          Log.d(TAG, "startBackgroundMonitoring: Background monitoring loop interrupted")
          if (isRunning()) {
            Log.d(TAG, "startBackgroundMonitoring: Service still running, restarting scan")
          } else {
            Thread.currentThread().interrupt()
            break
          }
        } catch (e: Throwable) {
          Log.e(TAG, "startBackgroundMonitoring: Error in background monitoring loop", e)
          try {
            Thread.sleep(5000L)
          } catch (_: Exception) {}
        }
      }
    }
  }

  private fun minServiceDurationOrNull(triggers: List<com.yassernull.shappky.data.models.TriggerModel>): Long? = triggers.mapNotNull { it.serviceDuration.takeIf { d -> d > 0L } }.minOrNull()
}
