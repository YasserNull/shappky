package com.yassernull.shappky.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.services.trigger.TriggerBackgroundMonitor
import com.yassernull.shappky.services.trigger.TriggerForegroundMonitor
import com.yassernull.shappky.services.trigger.TriggerMonitoringState
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

  private val sharedState = TriggerMonitoringState()

  private val screenStateReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      when (intent?.action) {
        Intent.ACTION_SCREEN_OFF -> {
          sharedState.pendingSleepEvent = true
          Log.d(TAG, "SCREEN_OFF received: pendingSleepEvent=true")
        }
        Intent.ACTION_SCREEN_ON -> {
          sharedState.pendingWakeEvent = true
          Log.d(TAG, "SCREEN_ON received: pendingWakeEvent=true")
        }
      }
    }
  }

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "onCreate: ShappkyTriggerService initialized")
    shellManager = ShellManager(this, handler, executor)

    stateTracker = SystemStateTracker(this)
    foregroundTracker = AppForegroundTracker()
    actionExecutor = TriggerActionExecutor(this, handler, executor, shellManager)
    ruleEvaluator = TriggerRuleEvaluator(this, actionExecutor, stateTracker, foregroundTracker, shellManager, handler, executor)
    actionExecutor.onServiceStateChanged = { ruleEvaluator.clearCooldowns() }

    TriggerForegroundMonitor(
      context = this,
      handler = handler,
      shellManager = shellManager,
      stateTracker = stateTracker,
      foregroundTracker = foregroundTracker,
      ruleEvaluator = ruleEvaluator,
      executor = triggerExecutor,
      sharedState = sharedState,
      isRunning = { isRunning },
    ).start()

    TriggerBackgroundMonitor(
      context = this,
      handler = handler,
      shellManager = shellManager,
      foregroundTracker = foregroundTracker,
      ruleEvaluator = ruleEvaluator,
      executor = backgroundExecutor,
      sharedState = sharedState,
      isRunning = { isRunning },
    ).start()

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

    val screenFilter = IntentFilter().apply {
      addAction(Intent.ACTION_SCREEN_ON)
      addAction(Intent.ACTION_SCREEN_OFF)
    }
    registerReceiver(screenStateReceiver, screenFilter)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d(TAG, "onStartCommand: ShappkyTriggerService starting sticky")
    return START_STICKY
  }

  override fun onDestroy() {
    isRunning = false
    try {
      unregisterReceiver(screenStateReceiver)
    } catch (_: Exception) {}
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

    @Volatile
    private var isRunning = false

    @JvmStatic
    fun isRunning(): Boolean = isRunning
  }
}
