package com.yn.shappky.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import com.yn.shappky.R
import com.yn.shappky.utils.ShellManager
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ShappkyService : Service() {
  private val executor: ExecutorService = Executors.newSingleThreadExecutor()
  private val handler = Handler(Looper.getMainLooper())
  private lateinit var shellManager: ShellManager

  override fun onCreate() {
    super.onCreate()
    shellManager = ShellManager(this, handler, executor)
    createNotificationChannel()

    val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.shappky_service))
      .setContentText(getString(R.string.shappky_service_notification_text))
      .setSmallIcon(R.drawable.ic_shappky)
      .setOngoing(true)
      .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
      startForeground(1, notification)
    }
    setRunningState(true)
    requestTileUpdate()
    startKillerLoop()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

  private fun startKillerLoop() {
    if (!executor.isShutdown) {
      executor.execute {
        while (isRunning) {
          try {
            killBackgroundApps()
            val sharedpreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val intervalMs = sharedpreferences.getLong("service_duration", 18000L)
            Thread.sleep(intervalMs)
          } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            break
          }
        }
      }
    }
  }

  private fun getUsedRamMb(): Int = try {
    val process = Runtime.getRuntime().exec("cat /proc/meminfo")
    BufferedReader(java.io.InputStreamReader(process.inputStream)).use { reader ->
      var memTotal = 0
      var memFree = 0
      var line = reader.readLine()
      while (line != null) {
        if (line.startsWith("MemTotal")) {
          memTotal = line.split(Regex("\\s+"))[1].toInt()
        } else if (line.startsWith("MemAvailable")) {
          memFree = line.split(Regex("\\s+"))[1].toInt()
        }
        line = reader.readLine()
      }
      process.waitFor()
      if (memTotal > 0 && memFree >= 0) (memTotal - memFree) / 1024 else 0
    }
  } catch (_: Exception) {
    0
  }

  private fun killBackgroundApps() {
    if (!shellManager.hasAnyShellPermission()) {
      shellManager.checkShellPermissions()
      return
    }

    val sharedpreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val killAllOnRamLimit = sharedpreferences.getBoolean("service_kill_all_on_ram_limit", false)
    val killAllRamThreshold = sharedpreferences.getInt("service_kill_all_ram_threshold", 0)

    if (killAllOnRamLimit && killAllRamThreshold > 0) {
      val usedRamMb = getUsedRamMb()
      if (usedRamMb < killAllRamThreshold) {
        return
      }
    }

    val selectUserApps = sharedpreferences.getBoolean("service_select_user_apps", true)
    val selectSystemApps = sharedpreferences.getBoolean("service_select_system_apps", false)
    val serviceExcludedApps = sharedpreferences.getStringSet("service_excluded_apps", emptySet()) ?: emptySet()
    val serviceManuallySelectedApps = sharedpreferences.getStringSet("service_manually_selected_apps", emptySet()) ?: emptySet()
    val killAppOnRamLimit = sharedpreferences.getBoolean("service_kill_app_on_ram_limit", false)
    val killAppRamThreshold = sharedpreferences.getInt("service_kill_app_ram_threshold", 0)

    val hiddenApps = sharedpreferences.getStringSet(KEY_HIDDEN_APPS, HashSet()) ?: HashSet()

    val protectedApps = com.yn.shappky.utils.ProtectionManager.getProtectedApps(this)

    val dumpOutput = shellManager.runShellCommandAndGetFullOutput("dumpsys activity activities") ?: return
    val psOutput =
      shellManager.runShellCommandAndGetFullOutput("ps -A -o rss,name | grep '\\.' | grep -v '[-@]'")
        ?: return

    val runningPackages = HashSet<String>()
    val packageRamUsage = HashMap<String, Int>()
    val pm = packageManager
    try {
      BufferedReader(StringReader(psOutput)).use { reader ->
        var line = reader.readLine()
        while (line != null) {
          val parts = line.trim().split(Regex("\\s+"))
          if (parts.size >= 2) {
            val rawPackageName = parts[1].trim()
            val packageName = if (rawPackageName.contains(":")) rawPackageName.substringBefore(":") else rawPackageName
            val rssKb = parts[0].trim().toLongOrNull() ?: 0L
            if (packageName.isNotEmpty() && packageName.contains(".")) {
              try {
                pm.getApplicationInfo(packageName, 0)
                runningPackages.add(packageName)
                packageRamUsage[packageName] = (packageRamUsage[packageName] ?: 0) + (rssKb / 1024L).toInt() // Convert to MB
              } catch (_: PackageManager.NameNotFoundException) {
              }
            }
          }
          line = reader.readLine()
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }

    val toKill = runningPackages.filter { pkg ->
      try {
        if (
          isProtected(pkg, protectedApps) ||
          dumpOutput.contains(pkg)
        ) {
          return@filter false
        }

        if (hiddenApps.contains(pkg)) {
          return@filter false
        }

        if (serviceExcludedApps.contains(pkg)) {
          return@filter false
        }

        if (serviceManuallySelectedApps.isNotEmpty() && !serviceManuallySelectedApps.contains(pkg)) {
          return@filter false
        }

        val appInfo = pm.getApplicationInfo(pkg, 0)
        val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0

        if (serviceManuallySelectedApps.isEmpty()) {
          val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
          if (isPersistent) {
            return@filter false
          }
          if (isSystem && !selectSystemApps) {
            return@filter false
          }
          if (!isSystem && !selectUserApps) {
            return@filter false
          }
        }

        if (serviceManuallySelectedApps.isNotEmpty()) {
          val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
          if (isPersistent) {
            return@filter false
          }
        }

        if (killAppOnRamLimit && killAppRamThreshold > 0) {
          val appRamMb = packageRamUsage[pkg] ?: 0
          if (appRamMb < killAppRamThreshold) {
            return@filter false
          }
        }

        true
      } catch (_: PackageManager.NameNotFoundException) {
        false
      }
    }

    if (toKill.isNotEmpty()) {
      val shouldKillAll = selectSystemApps || selectUserApps
      val finalCommand = com.yn.shappky.utils.BackgroundAppManager.buildSmartKillCommand(toKill, shouldKillAll)
      shellManager.runShellCommandAndGetFullOutput(finalCommand) ?: return

      val lines = toKill.map { pkg ->
        val ramMb = packageRamUsage[pkg] ?: 0
        val ramStr = getString(R.string.mb_format, ramMb.toFloat())
        getString(R.string.ram_freed_from_pkg, ramStr, pkg)
      }
      val contentText = lines.first()
      val bigText = lines.joinToString("\n")
      updateNotification(contentText, bigText)
    }
  }

  private fun isProtected(
    packageName: String,
    protectedApps: Set<String>,
  ): Boolean = packageName == "com.yn.shappky" || protectedApps.contains(packageName)

  override fun onDestroy() {
    setRunningState(false)
    requestTileUpdate()
    super.onDestroy()
    executor.shutdownNow()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun requestTileUpdate() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      TileService.requestListeningState(this, ComponentName(this, ShappkyQuickTile::class.java))
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        getString(R.string.shappky_service_channel),
        NotificationManager.IMPORTANCE_LOW,
      )
      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }

  private fun updateNotification(text: String, bigText: String? = null) {
    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(getString(R.string.shappky_service))
      .setContentText(text)
      .setSmallIcon(R.drawable.ic_shappky)
      .setOngoing(true)

    if (bigText != null) {
      builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
    }

    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(1, builder.build())
  }

  companion object {
    private var isRunning = false
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<(Boolean) -> Unit>()
    private const val CHANNEL_ID = "ShappkyChannel"
    private const val PREFERENCES_NAME = "AppPreferences"
    private const val KEY_HIDDEN_APPS = "hidden_apps"

    @JvmStatic
    fun isRunning(): Boolean = isRunning

    @JvmStatic
    fun registerListener(listener: (Boolean) -> Unit) {
      listeners.add(listener)
      listener(isRunning)
    }

    @JvmStatic
    fun unregisterListener(listener: (Boolean) -> Unit) {
      listeners.remove(listener)
    }

    private fun setRunningState(running: Boolean) {
      isRunning = running
      listeners.forEach { it(running) }
    }
  }
}
