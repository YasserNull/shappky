package com.yassernull.shappky.core.domain.evaluator

import android.content.Context
import android.os.Handler
import android.util.Log
import com.yassernull.shappky.R
import com.yassernull.shappky.core.domain.executors.TriggerActionExecutor
import com.yassernull.shappky.core.domain.trackers.AppForegroundTracker
import com.yassernull.shappky.core.domain.trackers.SystemStateTracker
import com.yassernull.shappky.core.managers.BackgroundAppManager
import com.yassernull.shappky.core.managers.ProtectionManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.managers.parseRecentsPackages
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.data.models.TriggerRule
import com.yassernull.shappky.services.ShappkyService
import com.yassernull.shappky.utils.NotificationUtils
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
    private const val APP_FOREGROUND_RULE_COOLDOWN_MS = 60000L
  }

  private val lastExecutedTime = mutableMapOf<String, Long>()
  private val recentShappkyKills = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

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
              if (stateTracker.hasServiceStateChanged(serviceKey)) {
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
            if (stateTracker.hasServiceStateChanged(serviceKey)) {
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
          if (!isShappkyServiceRunning) {
            actionExecutor.enableShappkyService(rule)
          } else {
            actionExecutor.disableShappkyService(rule)
          }
        }
      }
    }
  }

  fun evaluateAppForegroundRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    currentForeground: String?,
    previouslyForeground: String?,
  ) {
    val isShappkyServiceRunning = ShappkyService.isRunning()
    val now = System.currentTimeMillis()

    for (trigger in triggers) {
      val appOpenedRules = trigger.rules.filter { it.type == RuleType.APP_OPENED }
      if (currentForeground != null && appOpenedRules.any { rule -> rule.appPackages.contains(currentForeground) }) {
        val matchedRule = appOpenedRules.first { rule -> rule.appPackages.contains(currentForeground) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          Log.d(TAG, "APP_OPENED MATCH FOUND! Triggering '${trigger.name}' for $currentForeground")
          actionExecutor.executeServiceTrigger(trigger)
        }
      }

      val appResumedRules = trigger.rules.filter { it.type == RuleType.APP_RESUMED }
      if (currentForeground != null && appResumedRules.any { rule -> rule.appPackages.contains(currentForeground) }) {
        val matchedRule = appResumedRules.first { rule -> rule.appPackages.contains(currentForeground) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          Log.d(TAG, "APP_RESUMED MATCH FOUND! Triggering '${trigger.name}' for $currentForeground")
          actionExecutor.executeServiceTrigger(trigger)
        }
      }

      if (previouslyForeground != null) {
        val appClosedRules = trigger.rules.filter { it.type == RuleType.APP_CLOSED }
        if (appClosedRules.any { rule -> rule.appPackages.contains(previouslyForeground) }) {
          Log.d(TAG, "APP_CLOSED MATCH FOUND! Triggering '${trigger.name}' for $previouslyForeground")
          actionExecutor.executeServiceTrigger(trigger)
        }
      }
    }

    if (!isShappkyServiceRunning && enableRules.isNotEmpty()) {
      val appOpenedRules = enableRules.filter { it.type == RuleType.APP_OPENED }
      if (currentForeground != null && appOpenedRules.any { rule -> rule.appPackages.contains(currentForeground) }) {
        val matchedRule = appOpenedRules.first { rule -> rule.appPackages.contains(currentForeground) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          actionExecutor.enableShappkyService(matchedRule)
        }
      }

      val appResumedRules = enableRules.filter { it.type == RuleType.APP_RESUMED }
      if (currentForeground != null && appResumedRules.any { rule -> rule.appPackages.contains(currentForeground) }) {
        val matchedRule = appResumedRules.first { rule -> rule.appPackages.contains(currentForeground) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          actionExecutor.enableShappkyService(matchedRule)
        }
      }

      if (previouslyForeground != null) {
        val appClosedRules = enableRules.filter { it.type == RuleType.APP_CLOSED }
        if (appClosedRules.any { rule -> rule.appPackages.contains(previouslyForeground) }) {
          actionExecutor.enableShappkyService(appClosedRules.first())
        }
      }
    }

    if (isShappkyServiceRunning && disableRules.isNotEmpty()) {
      val appOpenedRules = disableRules.filter { it.type == RuleType.APP_OPENED }
      if (currentForeground != null && appOpenedRules.any { rule -> rule.appPackages.contains(currentForeground) }) {
        val matchedRule = appOpenedRules.first { rule -> rule.appPackages.contains(currentForeground) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          actionExecutor.disableShappkyService(matchedRule)
        }
      }

      val appResumedRules = disableRules.filter { it.type == RuleType.APP_RESUMED }
      if (currentForeground != null && appResumedRules.any { rule -> rule.appPackages.contains(currentForeground) }) {
        val matchedRule = appResumedRules.first { rule -> rule.appPackages.contains(currentForeground) }
        val lastRun = lastExecutedTime[matchedRule.id] ?: 0L
        if (now - lastRun >= APP_FOREGROUND_RULE_COOLDOWN_MS) {
          lastExecutedTime[matchedRule.id] = now
          actionExecutor.disableShappkyService(matchedRule)
        }
      }

      if (previouslyForeground != null) {
        val appClosedRules = disableRules.filter { it.type == RuleType.APP_CLOSED }
        if (appClosedRules.any { rule -> rule.appPackages.contains(previouslyForeground) }) {
          actionExecutor.disableShappkyService(appClosedRules.first())
        }
      }
    }
  }

  fun handleSleepAppClosedRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    prevApp: String,
  ) {
    val isShappkyServiceRunning = ShappkyService.isRunning()

    for (trigger in triggers) {
      val appClosedRules = trigger.rules.filter { it.type == RuleType.APP_CLOSED }
      if (appClosedRules.any { rule -> rule.appPackages.contains(prevApp) }) {
        Log.d(TAG, "APP_CLOSED MATCH FOUND (Sleep)! Triggering '${trigger.name}' for $prevApp")
        actionExecutor.executeServiceTrigger(trigger)
      }
    }
    if (!isShappkyServiceRunning && enableRules.isNotEmpty()) {
      val appClosedRules = enableRules.filter { it.type == RuleType.APP_CLOSED }
      if (appClosedRules.any { rule -> rule.appPackages.contains(prevApp) }) {
        actionExecutor.enableShappkyService(appClosedRules.first())
      }
    }
    if (isShappkyServiceRunning && disableRules.isNotEmpty()) {
      val appClosedRules = disableRules.filter { it.type == RuleType.APP_CLOSED }
      if (appClosedRules.any { rule -> rule.appPackages.contains(prevApp) }) {
        actionExecutor.disableShappkyService(appClosedRules.first())
      }
    }
  }

  fun evaluateAppKilledManually(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    killedPackages: Set<String>,
  ) {
    val isShappkyServiceRunning = ShappkyService.isRunning()

    for (pkg in killedPackages) {
      if (!recentShappkyKills.contains(pkg)) {
        for (trigger in triggers) {
          val manualKillRules = trigger.rules.filter { it.type == RuleType.APP_KILLED_MANUALLY }
          if (manualKillRules.any { rule -> rule.appPackages.contains(pkg) }) {
            Log.d(TAG, "APP_KILLED_MANUALLY MATCH FOUND! Triggering '${trigger.name}' for $pkg")
            actionExecutor.executeServiceTrigger(trigger)
          }
        }
        if (!isShappkyServiceRunning && enableRules.isNotEmpty()) {
          val manualKillRules = enableRules.filter { it.type == RuleType.APP_KILLED_MANUALLY }
          if (manualKillRules.any { rule -> rule.appPackages.contains(pkg) }) {
            actionExecutor.enableShappkyService(manualKillRules.first())
          }
        }
        if (isShappkyServiceRunning && disableRules.isNotEmpty()) {
          val manualKillRules = disableRules.filter { it.type == RuleType.APP_KILLED_MANUALLY }
          if (manualKillRules.any { rule -> rule.appPackages.contains(pkg) }) {
            actionExecutor.disableShappkyService(manualKillRules.first())
          }
        }
      }
    }
  }

  fun evaluateRamExceededRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    packageRamUsage: Map<String, Long>,
  ) {
    val isShappkyServiceRunning = ShappkyService.isRunning()
    val appManager = BackgroundAppManager(context, handler, executor, shellManager)

    for (trigger in triggers) {
      val ramExceededRules = trigger.rules.filter { it.type == RuleType.APP_RAM_EXCEEDED }
      if (ramExceededRules.isEmpty()) continue
      for (rule in ramExceededRules) {
        for (pkg in rule.appPackages) {
          val pkgRam = (packageRamUsage[pkg] ?: 0L) / 1024L
          if (pkgRam >= rule.ramThresholdMb && rule.ramThresholdMb > 0) {
            Log.d(TAG, "APP_RAM_EXCEEDED! $pkg is using $pkgRam MB")
            appManager.killPackages(listOf(pkg), {
              recentShappkyKills.add(pkg)
              val freedText = context.getString(R.string.free_up_memory, appManager.formatMemorySize(pkgRam * 1024))
              NotificationUtils.showTriggerFreedMemoryNotification(context, trigger.name, freedText)
            }, showToast = false)
          }
        }
      }
    }

    if (!isShappkyServiceRunning && enableRules.isNotEmpty()) {
      val ramExceededRules = enableRules.filter { it.type == RuleType.APP_RAM_EXCEEDED }
      for (rule in ramExceededRules) {
        for (pkg in rule.appPackages) {
          val pkgRam = (packageRamUsage[pkg] ?: 0L) / 1024L
          if (pkgRam >= rule.ramThresholdMb && rule.ramThresholdMb > 0) {
            actionExecutor.enableShappkyService(rule)
          }
        }
      }
    }

    if (isShappkyServiceRunning && disableRules.isNotEmpty()) {
      val ramExceededRules = disableRules.filter { it.type == RuleType.APP_RAM_EXCEEDED }
      for (rule in ramExceededRules) {
        for (pkg in rule.appPackages) {
          val pkgRam = (packageRamUsage[pkg] ?: 0L) / 1024L
          if (pkgRam >= rule.ramThresholdMb && rule.ramThresholdMb > 0) {
            actionExecutor.disableShappkyService(rule)
          }
        }
      }
    }
  }

  fun evaluateInactivityRules(
    triggers: List<TriggerModel>,
    enableRules: List<TriggerRule>,
    disableRules: List<TriggerRule>,
    runningPackages: Set<String>,
    currentForeground: String?,
    packageRamUsage: Map<String, Long>,
    now: Long,
  ) {
    val pm = context.packageManager
    val protectedApps = ProtectionManager.getProtectedApps(context)
    val isShappkyServiceRunning = ShappkyService.isRunning()
    val appManager = BackgroundAppManager(context, handler, executor, shellManager)

    for (trigger in triggers) {
      val inactivityRules = trigger.rules.filter { it.type == RuleType.APP_INACTIVITY }
      val killOldestRules = trigger.rules.filter { it.type == RuleType.KILL_OLDEST_APP }
      if (inactivityRules.isEmpty() && killOldestRules.isEmpty()) continue

      val selectUserApps = trigger.selectUserApps
      val selectSystemApps = trigger.selectSystemApps
      val selectPersistentApps = trigger.selectPersistentApps
      val excludedApps = trigger.excludedApps
      val manuallySelectedApps = trigger.manuallySelectedApps

      val candidatePackages = runningPackages.filter { pkg ->
        if (pkg == "com.yassernull.shappky" || protectedApps.contains(pkg) || ProtectionManager.isPackageProtected(context, pkg) || excludedApps.contains(pkg)) return@filter false
        val matchesManual = manuallySelectedApps.contains(pkg)
        if (matchesManual) return@filter true
        if (manuallySelectedApps.isNotEmpty()) return@filter false
        try {
          val appInfo = pm.getApplicationInfo(pkg, 0)
          val isSystem = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
          val isPersistent = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_PERSISTENT != 0
          val matchesUser = !isSystem && !isPersistent && selectUserApps
          val matchesSystem = isSystem && selectSystemApps
          val matchesPersistent = isPersistent && selectPersistentApps
          matchesUser || matchesSystem || matchesPersistent
        } catch (_: Exception) {
          false
        }
      }

      for (rule in killOldestRules) {
        val thresholdMs = rule.inactivityDurationMinutes * 60 * 1000L
        var oldestPkg: String? = null
        var maxInactiveDuration = -1L
        for (pkg in candidatePackages) {
          if (pkg == currentForeground) continue
          val lastActive = foregroundTracker.getLastActiveTime(pkg, now)
          val inactiveDuration = now - lastActive
          if (inactiveDuration >= thresholdMs && inactiveDuration > maxInactiveDuration) {
            maxInactiveDuration = inactiveDuration
            oldestPkg = pkg
          }
        }
        if (oldestPkg != null) {
          Log.d(TAG, "KILL_OLDEST_APP triggered! Killing $oldestPkg")
          appManager.killPackages(listOf(oldestPkg), {
            recentShappkyKills.add(oldestPkg)
            val totalKb = packageRamUsage[oldestPkg] ?: 0L
            val freedText = context.getString(R.string.free_up_memory, appManager.formatMemorySize(totalKb))
            NotificationUtils.showTriggerFreedMemoryNotification(context, trigger.name, freedText)
            foregroundTracker.removeRecord(oldestPkg)
          }, showToast = false)
        }
      }

      val packagesToKill = mutableListOf<String>()
      for (rule in inactivityRules) {
        val thresholdMs = rule.inactivityDurationMinutes * 60 * 1000L
        for (pkg in candidatePackages) {
          if (pkg == currentForeground) continue
          val lastActive = foregroundTracker.getLastActiveTime(pkg, now)
          val inactiveDuration = now - lastActive
          if (inactiveDuration >= thresholdMs) {
            packagesToKill.add(pkg)
          }
        }
      }

      if (packagesToKill.isNotEmpty()) {
        Log.d(TAG, "Inactivity rule triggered in trigger '${trigger.name}'. Killing: $packagesToKill")
        appManager.killPackages(packagesToKill, {
          recentShappkyKills.addAll(packagesToKill)
          val totalKb = packagesToKill.sumOf { packageRamUsage[it] ?: 0L }
          val freedText = context.getString(R.string.free_up_memory, appManager.formatMemorySize(totalKb))
          NotificationUtils.showTriggerFreedMemoryNotification(context, trigger.name, freedText)
          packagesToKill.forEach { foregroundTracker.removeRecord(it) }
        }, showToast = false)
      }
    }

    val candidatePackagesForServiceRules = runningPackages.filter { it != "com.yassernull.shappky" }

    if (isShappkyServiceRunning && disableRules.isNotEmpty()) {
      val inactivityDisableRules = disableRules.filter { it.type == RuleType.APP_INACTIVITY }
      for (rule in inactivityDisableRules) {
        val thresholdMs = rule.inactivityDurationMinutes * 60 * 1000L
        for (pkg in candidatePackagesForServiceRules) {
          if (rule.appPackages.contains(pkg)) {
            val lastActive = foregroundTracker.getLastActiveTime(pkg, now)
            if (now - lastActive >= thresholdMs) {
              actionExecutor.disableShappkyService(rule)
              break
            }
          }
        }
      }
    }

    if (!isShappkyServiceRunning && enableRules.isNotEmpty()) {
      val inactivityRules = enableRules.filter { it.type == RuleType.APP_INACTIVITY }
      for (rule in inactivityRules) {
        val thresholdMs = rule.inactivityDurationMinutes * 60 * 1000L
        for (pkg in candidatePackagesForServiceRules) {
          if (rule.appPackages.contains(pkg)) {
            val lastActive = foregroundTracker.getLastActiveTime(pkg, now)
            if (now - lastActive >= thresholdMs) {
              actionExecutor.enableShappkyService(rule)
              break
            }
          }
        }
      }
    }
  }

  fun evaluateAutoStartedBackgroundRules(
    triggers: List<TriggerModel>,
    runningPackages: Set<String>,
    packageRamUsage: Map<String, Long>,
  ) {
    if (runningPackages.isEmpty()) {
      Log.d(TAG, "evaluateAutoStartedBackgroundRules: skipped, no running packages")
      return
    }

    val pm = context.packageManager
    val protectedApps = ProtectionManager.getProtectedApps(context)

    // Packages the user opened (tasks still present in recents)
    val recentsOutput = shellManager.runShellCommandAndGetFullOutput("dumpsys activity recents") ?: ""
    if (recentsOutput.isBlank() || recentsOutput.startsWith("ERROR")) {
      Log.w(TAG, "evaluateAutoStartedBackgroundRules: dumpsys activity recents empty/error, skipping")
      return
    }
    val userLaunchedPackages = parseRecentsPackages(recentsOutput, pm)
    Log.d(TAG, "evaluateAutoStartedBackgroundRules: userLaunchedPackages=$userLaunchedPackages")

    for (trigger in triggers) {
      val bgRules = trigger.rules.filter { it.type == RuleType.APP_BACKGROUND_STARTED }
      if (bgRules.isEmpty()) continue

      // 1. Running packages matching the trigger (skip protected & persistent for sure)
      val runningMatches = runningPackages.filter { pkg ->
        if (pkg == context.packageName || protectedApps.contains(pkg) || ProtectionManager.isPackageProtected(context, pkg) || trigger.excludedApps.contains(pkg)) {
          Log.d(TAG, "evaluateAutoStartedBackgroundRules: '$pkg' excluded in trigger '${trigger.name}' (protected/excluded)")
          return@filter false
        }
        try {
          val appInfo = pm.getApplicationInfo(pkg, 0)
          val isSystem = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
          val isPersistent = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_PERSISTENT != 0
          if (isPersistent) {
            Log.d(TAG, "evaluateAutoStartedBackgroundRules: '$pkg' skipped (persistent)")
            return@filter false
          }
          if (trigger.manuallySelectedApps.contains(pkg)) return@filter true
          if (trigger.manuallySelectedApps.isNotEmpty()) {
            Log.d(TAG, "evaluateAutoStartedBackgroundRules: '$pkg' not in manual list of '${trigger.name}', skipped")
            return@filter false
          }
          val matchesUser = !isSystem && trigger.selectUserApps
          val matchesSystem = isSystem && trigger.selectSystemApps
          if (!matchesUser && !matchesSystem) {
            Log.d(TAG, "evaluateAutoStartedBackgroundRules: '$pkg' no filter match (system=$isSystem, user=${trigger.selectUserApps}, systemApps=${trigger.selectSystemApps})")
          }
          matchesUser || matchesSystem
        } catch (_: Exception) {
          false
        }
      }

      // 3. Auto-started = running and NOT present in recents (user never opened it)
      val autoStartedPackages = runningMatches - userLaunchedPackages
      Log.d(TAG, "evaluateAutoStartedBackgroundRules: trigger='${trigger.name}' runningMatches=$runningMatches, autoStartedPackages=$autoStartedPackages")
      if (autoStartedPackages.isNotEmpty()) {
        Log.d(TAG, "APP_BACKGROUND_STARTED MATCH FOUND! Triggering '${trigger.name}' for $autoStartedPackages")
        val appManager = BackgroundAppManager(context, handler, executor, shellManager)
        appManager.killPackages(autoStartedPackages.toList(), {
          recentShappkyKills.addAll(autoStartedPackages)
          val totalKb = autoStartedPackages.sumOf { packageRamUsage[it] ?: 0L }
          val freedText = context.getString(R.string.free_up_memory, appManager.formatMemorySize(totalKb))
          Log.d(TAG, "APP_BACKGROUND_STARTED kill completed for $autoStartedPackages, freed=$freedText")
          NotificationUtils.showTriggerFreedMemoryNotification(context, trigger.name, freedText)
        }, showToast = false)
      }
    }
  }

  fun cleanKilledApps(runningPackages: Set<String>) {
    val iteratorKill = recentShappkyKills.iterator()
    while (iteratorKill.hasNext()) {
      val pkg = iteratorKill.next()
      if (!runningPackages.contains(pkg)) {
        iteratorKill.remove()
      }
    }
  }
}
