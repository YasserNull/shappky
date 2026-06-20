package com.yn.shappky

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.yn.shappky.util.ShellManager
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ShappkyService : Service() {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val handler = Handler()
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

        startForeground(1, notification)
        isRunning = true
        requestTileUpdate()
        startKillerLoop()
    }

    private fun startKillerLoop() {
        executor.execute {
            while (isRunning) {
                try {
                    killBackgroundApps()
                    Thread.sleep(18000)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    private fun killBackgroundApps() {
        if (!shellManager.hasAnyShellPermission()) {
            shellManager.checkShellPermissions()
            handler.post {
                Toast.makeText(this, getString(R.string.shell_permission_required), Toast.LENGTH_SHORT).show()
            }
            return
        }

        val sharedpreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val hiddenApps = sharedpreferences.getStringSet(KEY_HIDDEN_APPS, HashSet()) ?: HashSet()

        val rawKeyboardPackage =
            Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val currentKeyboardPackage =
            if (rawKeyboardPackage != null && rawKeyboardPackage.contains("/")) {
                rawKeyboardPackage.split("/")[0]
            } else {
                null
            }

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val currentLauncherPackage = resolveInfo?.activityInfo?.packageName

        val dumpOutput = shellManager.runShellCommandAndGetFullOutput("dumpsys activity activities") ?: return
        val psOutput =
            shellManager.runShellCommandAndGetFullOutput("ps -A -o rss,name | grep '\\.' | grep -v '[-:@]' | awk '{print $2}'")
                ?: return

        val runningPackages = HashSet<String>()
        val pm = packageManager
        try {
            BufferedReader(StringReader(psOutput)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val packageName = line.trim()
                    if (packageName.isNotEmpty() && packageName.contains(".")) {
                        try {
                            pm.getApplicationInfo(packageName, 0)
                            runningPackages.add(packageName)
                        } catch (_: PackageManager.NameNotFoundException) {
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
                    hiddenApps.contains(pkg) ||
                    isProtected(pkg, currentKeyboardPackage, currentLauncherPackage) ||
                    dumpOutput.contains(pkg)
                ) {
                    return@filter false
                }
                val appInfo = pm.getApplicationInfo(pkg, 0)
                appInfo.flags and ApplicationInfo.FLAG_PERSISTENT == 0
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }

        if (toKill.isNotEmpty()) {
            val killCommand = toKill.joinToString("; ") { "am force-stop $it" }
            val finalCommand = "$killCommand; am kill-all"
            shellManager.runShellCommandAndGetFullOutput(finalCommand) ?: return
        } else {
            handler.post { Toast.makeText(this, getString(R.string.no_apps_to_kill), Toast.LENGTH_SHORT).show() }
        }
    }

    private fun isProtected(
        packageName: String,
        currentKeyboardPackage: String?,
        currentLauncherPackage: String?,
    ): Boolean = packageName == "com.yn.shappky" ||
        packageName == "com.google.android.gms" ||
        packageName == "com.android.systemui" ||
        packageName == "com.android.bluetooth" ||
        packageName == "com.android.externalstorage" ||
        packageName == "com.google.android.providers.media.module" ||
        packageName == "com.miui.miwallpaper" ||
        packageName == "com.android.camera" ||
        packageName == currentKeyboardPackage ||
        packageName == currentLauncherPackage

    override fun onDestroy() {
        isRunning = false
        requestTileUpdate()
        super.onDestroy()
        executor.shutdownNow()
        Toast.makeText(this, getString(R.string.service_stopped), Toast.LENGTH_SHORT).show()
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

    companion object {
        private var isRunning = false
        private const val CHANNEL_ID = "ShappkyChannel"
        private const val PREFERENCES_NAME = "AppPreferences"
        private const val KEY_HIDDEN_APPS = "hidden_apps"

        @JvmStatic
        fun isRunning(): Boolean = isRunning
    }
}
