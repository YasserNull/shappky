package com.yassernull.shappky.core.domain.evaluator

import android.content.Context
import android.os.Handler
import android.util.Log
import com.yassernull.shappky.core.domain.executors.TriggerActionExecutor
import com.yassernull.shappky.core.domain.trackers.AppForegroundTracker
import com.yassernull.shappky.core.domain.trackers.SystemStateTracker
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.data.models.TriggerRule
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

class TriggerRuleEvaluator(
  private val context: Context,
  private val actionExecutor: TriggerActionExecutor,
  private val stateTracker: SystemStateTracker,
  private val foregroundTracker: AppForegroundTracker,
  private val shellManager: ShellManager,
  private val handler: Handler,
  private val executor: ExecutorService,
) {
  companion object {
    private const val TAG = "TriggerRuleEvaluator"
  }

  private val lastExecutedTime = ConcurrentHashMap<String, Long>()
  private val recentlyReportedExits = ConcurrentHashMap<String, Long>()

  private val serviceStateEvaluator = ServiceStateRuleEvaluator(context, actionExecutor, stateTracker, lastExecutedTime)
  private val appLifecycleEvaluator = AppLifecycleRuleEvaluator(context, actionExecutor, foregroundTracker, lastExecutedTime, recentlyReportedExits)
  private val backgroundEvaluator = BackgroundRuleEvaluator(context, actionExecutor, foregroundTracker, shellManager, handler, executor)

  fun clearCooldowns() {
    lastExecutedTime.clear()
    Log.d(TAG, "Cooldowns cleared due to service state change")
  }

  fun evaluateServiceStateRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    isPhoneSleepTriggered: Boolean,
    isPhoneWakeTriggered: Boolean,
    usedMb: Long,
    now: Long,
  ) = serviceStateEvaluator.evaluateServiceStateRules(
    triggers,
    enableRules,
    disableRules,
    isPhoneSleepTriggered,
    isPhoneWakeTriggered,
    usedMb,
    now,
  )

  fun evaluateAppForegroundRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    currentForeground: String?,
    previouslyForeground: String?,
    currentForegroundWasKnown: Boolean,
  ) = appLifecycleEvaluator.evaluateAppForegroundRules(
    triggers,
    enableRules,
    disableRules,
    currentForeground,
    previouslyForeground,
    currentForegroundWasKnown,
  )

  fun handleSleepAppPausedRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    prevApp: String,
  ) = appLifecycleEvaluator.handleSleepAppPausedRules(triggers, enableRules, disableRules, prevApp)

  fun evaluateAppExitRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    killedPackages: Set<String>,
    swipedFromRecentsPackages: Set<String>,
  ) = appLifecycleEvaluator.evaluateAppExitRules(triggers, enableRules, disableRules, killedPackages, swipedFromRecentsPackages)

  fun evaluateRamExceededRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    packageRamUsage: Map<String, Long>,
  ) = backgroundEvaluator.evaluateRamExceededRules(triggers, enableRules, disableRules, packageRamUsage)

  fun evaluateInactivityRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    runningPackages: Set<String>,
    currentForeground: String?,
    packageRamUsage: Map<String, Long>,
    now: Long,
  ) = backgroundEvaluator.evaluateInactivityRules(
    triggers,
    enableRules,
    disableRules,
    runningPackages,
    currentForeground,
    packageRamUsage,
    now,
  )

  fun evaluateAutoStartedBackgroundRules(
    triggers: List<TriggerModel>,
    runningPackages: Set<String>,
    packageRamUsage: Map<String, Long>,
    userLaunchedPackages: Set<String>,
  ) = backgroundEvaluator.evaluateAutoStartedBackgroundRules(triggers, runningPackages, packageRamUsage, userLaunchedPackages)

  fun evaluateTriggerEnableDisableRules(
    triggers: List<TriggerModel>,
    isPhoneSleepTriggered: Boolean,
    isPhoneWakeTriggered: Boolean,
    usedMb: Long,
    now: Long,
  ) = serviceStateEvaluator.evaluateTriggerEnableDisableRules(triggers, isPhoneSleepTriggered, isPhoneWakeTriggered, usedMb, now)

  fun evaluateTriggerEnableDisableAppRules(
    triggers: List<TriggerModel>,
    currentForeground: String?,
    previouslyForeground: String?,
    currentForegroundWasKnown: Boolean,
    stoppedPackages: Set<String>,
    now: Long,
  ) = appLifecycleEvaluator.evaluateTriggerEnableDisableAppRules(
    triggers,
    currentForeground,
    previouslyForeground,
    currentForegroundWasKnown,
    stoppedPackages,
    now,
  )

  fun cleanKilledApps(runningPackages: Set<String>) = appLifecycleEvaluator.cleanKilledApps(runningPackages)
}
