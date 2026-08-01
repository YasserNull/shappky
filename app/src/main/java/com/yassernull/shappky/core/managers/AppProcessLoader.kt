package com.yassernull.shappky.core.managers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.AppModel
import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.function.Consumer

class AppProcessLoader(
  private val context: Context,
  private val handler: Handler,
  private val executor: ExecutorService,
  private val shellManager: ShellManager,
) {
  val currentAppsList = mutableListOf<AppModel>()
  var showUserApps = true
  var showSystemApps = true
  var showPersistentApps = false
  var showProtectedApps = false
  private val sharedPreferences =
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  @Volatile
  private var isCurrentlyLoadingApps = false

  private val pendingCallbacks = java.util.Collections.synchronizedList(ArrayList<Consumer<List<AppModel>>>())

  @Volatile
  private var isCurrentlyLoadingRam = false

  fun formatMemorySize(kb: Long): String = when {
    kb < 1024 -> context.getString(R.string.kb_format, kb)
    kb < 1024 * 1024 -> context.getString(R.string.mb_format, kb / 1024f)
    else -> context.getString(R.string.gb_format, kb / (1024f * 1024f))
  }

  fun getActiveWidgetPackages(): Set<String> {
    if (!shellManager.isShellCommandReady()) return emptySet()
    val activePackages = mutableSetOf<String>()
    try {
      val output = shellManager.runShellCommandAndGetFullOutput("dumpsys appwidget") ?: ""
      val regex = Regex("provider=ComponentInfo\\{([^/]+)/")
      var inAppWidgetIds = false
      for (line in output.split('\n')) {
        val trimmed = line.trim()
        if (trimmed == "AppWidgetIds:") {
          inAppWidgetIds = true
          continue
        } else if (line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) {
          if (inAppWidgetIds && !line.contains("AppWidgetIds")) {
            inAppWidgetIds = false
          }
        }
        if (inAppWidgetIds) {
          val match = regex.find(line)
          if (match != null) {
            activePackages.add(match.groupValues[1])
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting active widget packages", e)
    }
    return activePackages
  }

  fun loadBackgroundApps(callback: Consumer<List<AppModel>>?) {
    if (callback != null) {
      synchronized(pendingCallbacks) {
        pendingCallbacks.add(callback)
      }
    }
    if (isCurrentlyLoadingApps) {
      Log.d(TAG, "loadBackgroundApps skipped because another load is already in progress")
      return
    }
    isCurrentlyLoadingApps = true
    if (!executor.isShutdown) {
      executor.execute {
        val startTime = System.currentTimeMillis()
        Log.d(
          TAG,
          "loadBackgroundApps started showSystemApps=$showSystemApps, showPersistentApps=$showPersistentApps",
        )
        var result = mutableListOf<AppModel>()
        try {
          val hiddenApps = getHiddenApps()
          val protectedApps = ProtectionManager.getProtectedApps(context)

          if (shellManager.isShellCommandReady()) {
            val command = "${ShellManager.TOYBOX_PATH} ps -A -o rss,name,uid"
            try {
              val fullOutput = shellManager.runShellCommandAndGetFullOutput(command)
              if (fullOutput != null) {
                val entries = parsePsOutputToEntries(fullOutput)
                val pm = context.packageManager
                val packageRamMap = aggregateByPackage(entries)
                val validatedEntries = packageRamMap.filterKeys { pkg ->
                  try {
                    pm.getApplicationInfo(pkg, 0)
                    true
                  } catch (_: PackageManager.NameNotFoundException) {
                    false
                  }
                }
                val runningEntries = validatedEntries.map { "${it.key}:${it.value}" }.toSet()

                result = AppModelFilter.buildRunningAppModels(
                  runningEntries = runningEntries,
                  hiddenApps = hiddenApps,
                  protectedApps = protectedApps,
                  showUserApps = showUserApps,
                  showSystemApps = showSystemApps,
                  showPersistentApps = showPersistentApps,
                  showProtectedApps = showProtectedApps,
                  context = context,
                  formatMemorySize = ::formatMemorySize,
                ).toMutableList()
              }
            } catch (e: Exception) {
              Log.e(TAG, "Error getting running apps", e)
              handler.post {
                Toast.makeText(
                  context,
                  context.getString(R.string.error_getting_running_apps, e.message.orEmpty()),
                  Toast.LENGTH_SHORT,
                ).show()
              }
            }
          } else {
            Log.w(TAG, "Shell command backend is not ready while loading background apps")
          }

          result.sortWith(
            compareBy<AppModel> { it.isSystemApp }
              .thenBy { it.isPersistentApp }
              .thenBy { it.appName.lowercase(Locale.getDefault()) },
          )
        } catch (t: Throwable) {
          Log.e(TAG, "Fatal error in loadBackgroundApps background thread", t)
        } finally {
          isCurrentlyLoadingApps = false
          handler.post {
            Log.d(
              TAG,
              "loadBackgroundApps finished resultSize=${result.size}, durationMs=${System.currentTimeMillis() - startTime}",
            )
            currentAppsList.clear()
            currentAppsList.addAll(result)

            val callbacksToTrigger = synchronized(pendingCallbacks) {
              val list = ArrayList(pendingCallbacks)
              pendingCallbacks.clear()
              list
            }
            val immutableResult = ArrayList(result)
            for (cb in callbacksToTrigger) {
              cb.accept(immutableResult)
            }
          }
        }
      }
    }
  }

  fun loadAllApps(callback: Consumer<List<AppModel>>) {
    if (!executor.isShutdown) {
      executor.execute {
        val startTime = System.currentTimeMillis()
        val protectedApps = ProtectionManager.getProtectedApps(context)
        val allApps = AppModelFilter.buildAllAppsList(context, protectedApps)
        Log.d(TAG, "loadAllApps finished count=${allApps.size}, durationMs=${System.currentTimeMillis() - startTime}")
        handler.post { callback.accept(allApps) }
      }
    }
  }

  fun loadAppsRamUsage(packageNames: List<String>, callback: Consumer<Map<String, Long>>) {
    if (isCurrentlyLoadingRam) {
      Log.d(TAG, "loadAppsRamUsage skipped because another RAM load is in progress")
      return
    }
    isCurrentlyLoadingRam = true
    if (!executor.isShutdown) {
      executor.execute {
        val startTime = System.currentTimeMillis()
        val requestedPackages = packageNames.toSet()
        val ramUsageByPackage = mutableMapOf<String, Long>()
        try {
          if (requestedPackages.isNotEmpty() && shellManager.isShellCommandReady()) {
            val command = "${ShellManager.TOYBOX_PATH} ps -A -o rss,name,uid"
            try {
              val fullOutput = shellManager.runShellCommandAndGetFullOutput(command)
              if (fullOutput != null) {
                val entries = parsePsOutputToEntries(fullOutput)
                val aggregated = aggregateByPackage(entries)
                for ((pkg, rss) in aggregated) {
                  if (pkg in requestedPackages) {
                    ramUsageByPackage[pkg] = rss
                  }
                }
              }
            } catch (e: Exception) {
              Log.e(TAG, "Error updating app RAM usage", e)
            }
          }
          handler.post { callback.accept(ramUsageByPackage) }
        } catch (t: Throwable) {
          Log.e(TAG, "Fatal error in loadAppsRamUsage background thread", t)
        } finally {
          isCurrentlyLoadingRam = false
        }
      }
    }
  }

  fun loadAppDetailedInfo(app: AppModel, callback: Consumer<com.yassernull.shappky.data.models.AppDetailedInfo>) {
    if (!executor.isShutdown) {
      executor.execute {
        try {
          if (shellManager.isShellCommandReady()) {
            var fullOutput: String? = null
            var uidFiltered = false
            val appUid = resolveAppUid(app.packageName)
            Log.d(TAG, "loadAppDetailedInfo package=${app.packageName}, resolvedUid=$appUid")
            if (appUid != null) {
              val uidCommand = "${ShellManager.TOYBOX_PATH} ps -A -o pid,user,rss,name,uid"
              val uidOutput = shellManager.runShellCommandAndGetFullOutput(uidCommand)
              if (!uidOutput.isNullOrBlank()) {
                fullOutput = uidOutput
                uidFiltered = true
                Log.d(TAG, "loadAppDetailedInfo full ps output lines=${uidOutput.lines().size}")
              } else {
                Log.w(TAG, "loadAppDetailedInfo ps returned no output, falling back to package name grep")
              }
            }
            if (!uidFiltered) {
              fullOutput = shellManager.runShellCommandAndGetFullOutput(
                "${ShellManager.TOYBOX_PATH} ps -A -o pid,user,rss,name | grep '\\.' | grep -v '[-@]' | grep '" + app.packageName + "'",
              )
            }
            var processes = mutableListOf<com.yassernull.shappky.data.models.ProcessInfo>()
            var mainPid = "-"
            var mainUser = "-"
            var totalCpu = 0.0
            var totalThreads = 0
            var totalRam = 0L
            var isForeground = false

            if (fullOutput != null) {
              processes = parsePsOutputToProcessInfos(fullOutput, app.packageName, if (uidFiltered) appUid else null).toMutableList()
              Log.d(TAG, "loadAppDetailedInfo package=${app.packageName}, uidFiltered=$uidFiltered, parsedProcesses=${processes.size}, output=${fullOutput.trim().replace('\n', '|')}")

              var mainFound = false
              for (p in processes) {
                if (p.name == app.packageName) {
                  mainPid = p.pid
                  mainUser = "-"
                  mainFound = true
                }
                totalRam += p.ramKb
                try {
                  val statOutput = shellManager.runShellCommandAndGetFullOutput("cat /proc/${p.pid}/stat")
                  if (statOutput != null) {
                    totalThreads += parseStatForThreads(statOutput)
                  }
                } catch (_: Exception) {}
              }
              if (!mainFound && processes.isNotEmpty()) {
                mainPid = processes[0].pid
                mainUser = "N/A"
              }

              BufferedReader(java.io.StringReader(fullOutput)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                  val parts = line.trim().split(Regex("\\s+"))
                  if (parts.size >= 4 && !line.startsWith("ERROR:")) {
                    val user = parts[1]
                    val name = parts[3]
                    if (name == app.packageName || name.startsWith(app.packageName + ":")) {
                      if (name == app.packageName) mainUser = user
                    }
                  }
                  line = reader.readLine()
                }
              }

              try {
                val cpuOutput = shellManager.runShellCommandAndGetFullOutput("dumpsys cpuinfo | grep " + app.packageName)
                if (cpuOutput != null && !cpuOutput.startsWith("ERROR")) {
                  totalCpu = parseCpuInfoOutput(cpuOutput)
                }
              } catch (_: Exception) {}
            }

            val dumpCommand = "dumpsys activity services " + app.packageName + " | grep isForeground=true"
            val dumpOutput = shellManager.runShellCommandAndGetFullOutput(dumpCommand)
            isForeground = !dumpOutput.isNullOrBlank() && !dumpOutput.startsWith("ERROR")

            if (mainPid == "-" && processes.isNotEmpty()) {
              mainPid = processes[0].pid
              mainUser = "N/A"
            }

            val result = com.yassernull.shappky.data.models.AppDetailedInfo(
              app = app,
              pid = mainPid,
              user = mainUser,
              isForeground = isForeground,
              cpuUsage = String.format(java.util.Locale.US, "%.1f%%", totalCpu),
              threads = totalThreads.toString(),
              totalRamKb = totalRam,
              processes = processes,
            )
            handler.post { callback.accept(result) }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error loading detailed info", e)
        }
      }
    }
  }

  private fun resolveAppUid(packageName: String): String? = try {
    context.packageManager.getApplicationInfo(packageName, 0).uid.toString()
  } catch (_: PackageManager.NameNotFoundException) {
    null
  }

  fun getHiddenApps(): Set<String> = sharedPreferences.getStringSet(KEY_HIDDEN_APPS, HashSet()) ?: HashSet()

  fun saveHiddenApps(hiddenApps: Set<String>) {
    sharedPreferences.edit().putStringSet(KEY_HIDDEN_APPS, hiddenApps).apply()
  }

  fun getAppsList(): List<AppModel> = ArrayList(currentAppsList)

  companion object {
    private const val TAG = "ShappkyApps"
    const val PREFERENCES_NAME = "AppPreferences"
    const val KEY_HIDDEN_APPS = "hidden_apps"
  }
}
