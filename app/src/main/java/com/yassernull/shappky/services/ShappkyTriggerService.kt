package com.yassernull.shappky.services

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.yassernull.shappky.R
import com.yassernull.shappky.core.domain.evaluator.TriggerRuleEvaluator
import com.yassernull.shappky.core.domain.executors.TriggerActionExecutor
import com.yassernull.shappky.core.domain.trackers.AppForegroundTracker
import com.yassernull.shappky.core.domain.trackers.SystemStateTracker
import com.yassernull.shappky.core.managers.DisableTriggerManager
import com.yassernull.shappky.core.managers.EnableTriggerManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.managers.TriggerManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class ShappkyTriggerService : Service() {
  private val executor: ExecutorService = Executors.newSingleThreadExecutor()
  private val triggerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var shellManager: ShellManager

  private lateinit var stateTracker: SystemStateTracker
  private lateinit var foregroundTracker: AppForegroundTracker
  private lateinit var actionExecutor: TriggerActionExecutor
  private lateinit var ruleEvaluator: TriggerRuleEvaluator

  @Volatile
  private var lastSharedForeground: String? = null
  private val previousRunningPackages = mutableSetOf<String>()

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "onCreate: ShappkyTriggerService initialized")
    shellManager = ShellManager(this, handler, executor)

    stateTracker = SystemStateTracker(this)
    foregroundTracker = AppForegroundTracker()
    actionExecutor = TriggerActionExecutor(this, handler, executor, shellManager)
    ruleEvaluator = TriggerRuleEvaluator(this, actionExecutor, stateTracker, foregroundTracker, shellManager, handler, executor)

    createNotificationChannel()

    val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.trigger_channel_name))
      .setContentText(getString(R.string.trigger_service_active))
      .setSmallIcon(R.drawable.ic_shappky)
      .setOngoing(true)
      .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
      startForeground(2, notification)
    }
    isRunning = true
    startTriggerMonitoring()
    startBackgroundMonitoring()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "onStartCommand: ShappkyTriggerService starting sticky")
    return START_STICKY
  }

  private fun startTriggerMonitoring() {
    stateTracker.initializeStates()

    triggerExecutor.execute {
      while (isRunning) {
        try {
          val triggers = TriggerManager.getTriggers(this@ShappkyTriggerService)
          val activeTriggers = triggers.filter { it.isEnabled }
          val enableRules = EnableTriggerManager.getEnableRules(this@ShappkyTriggerService)
          val disableRules = DisableTriggerManager.getDisableRules(this@ShappkyTriggerService)

          val isShappkyServiceRunning = ShappkyService.isRunning()
          val hasWorkToDo = activeTriggers.isNotEmpty() ||
            (!isShappkyServiceRunning && enableRules.isNotEmpty()) ||
            (isShappkyServiceRunning && disableRules.isNotEmpty())

          if (!hasWorkToDo) {
            Thread.sleep(10000L)
            continue
          }

          val scanIntervalMs = minServiceDurationOrNull(activeTriggers) ?: 10000L

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

          val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
          val memoryInfo = ActivityManager.MemoryInfo()
          activityManager?.getMemoryInfo(memoryInfo)
          val totalMb = memoryInfo.totalMem / (1024 * 1024)
          val availMb = memoryInfo.availMem / (1024 * 1024)
          val usedMb = totalMb - availMb

          // 1. Evaluate Service States and General Rules
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
          val hasInactivityRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_INACTIVITY } } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_INACTIVITY })
          val hasAutoBgRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_BACKGROUND_STARTED } }
          val hasAppOpenedRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_OPENED } } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_OPENED })

          var currentForeground: String? = null
          if (stateTracker.currentInteractive && (hasAppOpenedRules || hasInactivityRules || hasAutoBgRules)) {
            if (shellManager.isShellCommandReady()) {
              val dumpOutput = shellManager.runShellCommandAndGetFullOutput("dumpsys activity activities")
              if (dumpOutput != null) {
                currentForeground = foregroundTracker.getForegroundPackage(dumpOutput)
                val previouslyForeground = foregroundTracker.updateForegroundApp(currentForeground, now)

                ruleEvaluator.evaluateAppForegroundRules(
                  activeTriggers,
                  enableRules,
                  disableRules,
                  currentForeground,
                  previouslyForeground,
                )
              }
            }
          }
          lastSharedForeground = currentForeground

          if (isPhoneSleepTriggered) {
            foregroundTracker.lastForegroundApp?.let { prevApp ->
              foregroundTracker.markAppAsInactive(prevApp, now)
              ruleEvaluator.handleSleepAppClosedRules(activeTriggers, enableRules, disableRules, prevApp)
            }
            foregroundTracker.lastForegroundApp = null
          }

          // 3. Background / RAM / Inactivity / Killed manually are evaluated by startBackgroundMonitoring()
          //    at an interval driven by the triggers' service duration.

          Thread.sleep(scanIntervalMs)
        } catch (_: InterruptedException) {
          Log.d(TAG, "startTriggerMonitoring: Monitoring loop interrupted")
          Thread.currentThread().interrupt()
          break
        } catch (e: Exception) {
          Log.e(TAG, "startTriggerMonitoring: Error in monitoring loop", e)
          try {
            Thread.sleep(5000L)
          } catch (_: Exception) {}
        }
      }
    }
  }

  private fun startBackgroundMonitoring() {
    backgroundExecutor.execute {
      while (isRunning) {
        try {
          val triggers = TriggerManager.getTriggers(this@ShappkyTriggerService)
          val activeTriggers = triggers.filter { it.isEnabled }
          val enableRules = EnableTriggerManager.getEnableRules(this@ShappkyTriggerService)
          val disableRules = DisableTriggerManager.getDisableRules(this@ShappkyTriggerService)

          val isShappkyServiceRunning = ShappkyService.isRunning()
          val hasInactivityRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_INACTIVITY } } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yassernull.shappky.data.models.RuleType.APP_INACTIVITY })
          val hasAutoBgRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.APP_BACKGROUND_STARTED } }
          val hasKillOldestRules = activeTriggers.any { it.rules.any { r -> r.type == com.yassernull.shappky.data.models.RuleType.KILL_OLDEST_APP } }

          if (!hasInactivityRules && !hasAutoBgRules && !hasKillOldestRules) {
            Thread.sleep(DEFAULT_BACKGROUND_INTERVAL_MS)
            continue
          }

          // Effective scan interval = smallest service duration among active triggers (fallback default)
          val intervalMs = minServiceDurationOrNull(activeTriggers) ?: DEFAULT_BACKGROUND_INTERVAL_MS
          Log.d(TAG, "startBackgroundMonitoring: scan in ${intervalMs}ms (inactivity=$hasInactivityRules, autoBg=$hasAutoBgRules, killOldest=$hasKillOldestRules)")

          Thread.sleep(intervalMs)
          if (!isRunning) break

          val now = System.currentTimeMillis()
          if (shellManager.isShellCommandReady()) {
            val psOutput = shellManager.runShellCommandAndGetFullOutput(com.yassernull.shappky.core.managers.psAllProcessesCommand())
            if (psOutput != null) {
              val packageUsages = com.yassernull.shappky.core.managers.aggregatePsOutputToPackages(psOutput, packageManager)
              val runningPackages = packageUsages.keys.toMutableSet()
              val packageRamUsage = packageUsages.mapValues { it.value.ramKb }
              val currentForeground = lastSharedForeground
              Log.d(TAG, "startBackgroundMonitoring: running=${runningPackages.size}, previous=${previousRunningPackages.size}, foreground=$currentForeground, added=${runningPackages - previousRunningPackages}")

              foregroundTracker.cleanUpOldForegroundRecords(runningPackages, currentForeground)
              foregroundTracker.initNewRunningPackages(runningPackages, currentForeground, now)
              ruleEvaluator.cleanKilledApps(runningPackages)

              // Check APP_KILLED_MANUALLY
              if (previousRunningPackages.isNotEmpty()) {
                val killedPackages = previousRunningPackages - runningPackages
                ruleEvaluator.evaluateAppKilledManually(activeTriggers, enableRules, disableRules, killedPackages)
              }

              // Check APP_BACKGROUND_STARTED (running apps the user never opened - not present in recents)
              if (hasAutoBgRules) {
                ruleEvaluator.evaluateAutoStartedBackgroundRules(
                  activeTriggers,
                  runningPackages,
                  packageRamUsage,
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
          Thread.currentThread().interrupt()
          break
        } catch (e: Exception) {
          Log.e(TAG, "startBackgroundMonitoring: Error in background monitoring loop", e)
          try {
            Thread.sleep(5000L)
          } catch (_: Exception) {}
        }
      }
    }
  }

  private fun minServiceDurationOrNull(triggers: List<com.yassernull.shappky.data.models.TriggerModel>): Long? = triggers.mapNotNull { it.serviceDuration.takeIf { d -> d > 0L } }.minOrNull()

  override fun onDestroy() {
    isRunning = false
    super.onDestroy()
    triggerExecutor.shutdownNow()
    backgroundExecutor.shutdownNow()
    executor.shutdownNow()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "خدمة مشغلات شابكي",
        NotificationManager.IMPORTANCE_LOW,
      )
      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }

  companion object {
    private const val TAG = "ShappkyTriggerService"
    private const val CHANNEL_ID = "ShappkyTriggerChannel"
    private const val DEFAULT_BACKGROUND_INTERVAL_MS = 50000L

    @Volatile
    private var isRunning = false

    @JvmStatic
    fun isRunning(): Boolean = isRunning
  }
}
