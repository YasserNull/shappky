package com.yn.shappky.services

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
import com.yn.shappky.R
import com.yn.shappky.utils.DisableTriggerManager
import com.yn.shappky.utils.EnableTriggerManager
import com.yn.shappky.utils.ShellManager
import com.yn.shappky.utils.TriggerManager
import com.yn.shappky.utils.triggers.AppForegroundTracker
import com.yn.shappky.utils.triggers.SystemStateTracker
import com.yn.shappky.utils.triggers.TriggerActionExecutor
import com.yn.shappky.utils.triggers.TriggerRuleEvaluator
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("MissingPermission")
class ShappkyTriggerService : Service() {
  private val executor: ExecutorService = Executors.newSingleThreadExecutor()
  private val triggerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var shellManager: ShellManager

  private lateinit var stateTracker: SystemStateTracker
  private lateinit var foregroundTracker: AppForegroundTracker
  private lateinit var actionExecutor: TriggerActionExecutor
  private lateinit var ruleEvaluator: TriggerRuleEvaluator

  private var inactivityCheckCounter = 0
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
            Thread.sleep(2000L)
            continue
          }

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
          val hasInactivityRules = activeTriggers.any { it.rules.any { r -> r.type == com.yn.shappky.data.models.RuleType.APP_INACTIVITY } } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yn.shappky.data.models.RuleType.APP_INACTIVITY })
          val hasAppOpenedRules = activeTriggers.any { it.rules.any { r -> r.type == com.yn.shappky.data.models.RuleType.APP_OPENED } } || (!isShappkyServiceRunning && enableRules.any { it.type == com.yn.shappky.data.models.RuleType.APP_OPENED })

          var currentForeground: String? = null
          if (stateTracker.currentInteractive && (hasAppOpenedRules || hasInactivityRules)) {
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

          if (isPhoneSleepTriggered) {
            foregroundTracker.lastForegroundApp?.let { prevApp ->
              foregroundTracker.markAppAsInactive(prevApp, now)
              ruleEvaluator.handleSleepAppClosedRules(activeTriggers, enableRules, disableRules, prevApp)
            }
            foregroundTracker.lastForegroundApp = null
          }

          // 3. Evaluate background / RAM / Inactivity / Killed manually
          inactivityCheckCounter++
          if (hasInactivityRules && inactivityCheckCounter >= 5) {
            inactivityCheckCounter = 0
            if (shellManager.isShellCommandReady()) {
              val psOutput = shellManager.runShellCommandAndGetFullOutput("ps -A -o rss,name | grep '\\.' | grep -v '[-@]'")
              if (psOutput != null) {
                val runningPackages = mutableSetOf<String>()
                val packageRamUsage = mutableMapOf<String, Long>()

                java.io.BufferedReader(java.io.StringReader(psOutput)).use { reader ->
                  var line = reader.readLine()
                  while (line != null) {
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 2) {
                      val rawPkg = parts[1].trim()
                      val pkg = if (rawPkg.contains(":")) rawPkg.substringBefore(":") else rawPkg
                      val rssKb = parts[0].trim().toLongOrNull() ?: 0L
                      if (pkg.isNotEmpty() && pkg.contains(".")) {
                        runningPackages.add(pkg)
                        packageRamUsage[pkg] = (packageRamUsage[pkg] ?: 0L) + rssKb
                      }
                    }
                    line = reader.readLine()
                  }
                }

                foregroundTracker.cleanUpOldForegroundRecords(runningPackages, currentForeground)
                foregroundTracker.initNewRunningPackages(runningPackages, currentForeground, now)
                ruleEvaluator.cleanKilledApps(runningPackages)

                // Check APP_KILLED_MANUALLY
                if (previousRunningPackages.isNotEmpty()) {
                  val killedPackages = previousRunningPackages - runningPackages
                  ruleEvaluator.evaluateAppKilledManually(activeTriggers, enableRules, disableRules, killedPackages)
                }
                previousRunningPackages.clear()
                previousRunningPackages.addAll(runningPackages)

                // Process APP_RAM_EXCEEDED
                ruleEvaluator.evaluateRamExceededRules(activeTriggers, enableRules, disableRules, packageRamUsage)

                // Process Inactivity rules
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
          }

          Thread.sleep(2000L)
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

  override fun onDestroy() {
    isRunning = false
    super.onDestroy()
    triggerExecutor.shutdownNow()
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

    @Volatile
    private var isRunning = false

    @JvmStatic
    fun isRunning(): Boolean = isRunning
  }
}
