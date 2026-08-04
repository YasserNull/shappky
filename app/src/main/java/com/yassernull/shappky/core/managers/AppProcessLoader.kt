package com.yassernull.shappky.core.managers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.AppModel
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

  private val installedPackages: Set<String> by lazy {
    try {
      context.packageManager.getInstalledApplications(0).mapTo(mutableSetOf()) { it.packageName }
    } catch (_: Exception) {
      emptySet()
    }
  }

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
            val command = "${ShellManager.TOYBOX_PATH} ps -A -o %cpu,pid,user,rss,name,uid"
            try {
              val fullOutput = shellManager.runShellCommandAndGetFullOutput(command)
              if (fullOutput != null) {
                val entries = parsePsOutputToProcessEntries(fullOutput)
                val pm = context.packageManager
                val packageUsageMap = aggregateByPackage(entries, pm)
                val validatedEntries = packageUsageMap.filterKeys { pkg ->
                  try {
                    pm.getApplicationInfo(pkg, 0)
                    true
                  } catch (_: PackageManager.NameNotFoundException) {
                    false
                  }
                }
                val runningEntries = validatedEntries.map { (pkg, usage) -> "$pkg:${usage.ramKb}:${usage.cpuPercent}" }.toSet()
                Log.d(TAG, "loadBackgroundApps psLines=${fullOutput.lines().size}, parsedEntries=${entries.size}, packages=${packageUsageMap.size}, validated=${validatedEntries.size}")
                Log.d(TAG, "loadBackgroundApps aggregated=${packageUsageMap.entries.joinToString { "${it.key}=${it.value.ramKb}KB" }}")

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
            val command = "${ShellManager.TOYBOX_PATH} ps -A -o %cpu,pid,user,rss,name,uid"
            try {
              val fullOutput = shellManager.runShellCommandAndGetFullOutput(command)
              if (fullOutput != null) {
                val entries = parsePsOutputToProcessEntries(fullOutput)
                val aggregated = aggregateByPackage(entries, context.packageManager)
                Log.d(TAG, "loadAppsRamUsage aggregated=${aggregated.entries.joinToString { "${it.key}=${it.value.ramKb}KB" }}")
                for ((pkg, usage) in aggregated) {
                  if (pkg in requestedPackages) {
                    ramUsageByPackage[pkg] = usage.ramKb
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
            val appUid = resolveAppUid(app.packageName)
            Log.d(TAG, "loadAppDetailedInfo package=${app.packageName}, resolvedUid=$appUid")
            if (appUid != null) {
              val uidCommand = "${ShellManager.TOYBOX_PATH} ps -A -o %cpu,pid,user,rss,name,uid"
              val uidOutput = shellManager.runShellCommandAndGetFullOutput(uidCommand)
              if (!uidOutput.isNullOrBlank()) {
                fullOutput = uidOutput
                Log.d(TAG, "loadAppDetailedInfo full ps output lines=${uidOutput.lines().size}")
              } else {
                Log.w(TAG, "loadAppDetailedInfo ps returned no output, falling back to package name grep")
              }
            }
            if (fullOutput == null) {
              val escapedPkg = app.packageName.replace(".", "\\.")
              fullOutput = shellManager.runShellCommandAndGetFullOutput(
                "${ShellManager.TOYBOX_PATH} ps -A -o %cpu,pid,user,rss,name,uid | grep '\\.' | grep -v '[-@]' | grep -E '" + escapedPkg + "([^A-Za-z0-9]|\$)'",
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
              processes = parsePsOutputToProcessInfos(fullOutput, app.packageName, appUid).toMutableList()
              Log.d(TAG, "loadAppDetailedInfo package=${app.packageName}, uidFiltered=${appUid != null}, parsedProcesses=${processes.size}, output=${fullOutput.trim().replace('\n', '|')}")

              var mainFound = false
              for (p in processes) {
                if (p.name == app.packageName) {
                  mainPid = p.pid
                  mainUser = p.user
                  mainFound = true
                }
                totalRam += p.ramKb
                totalCpu += p.cpuPercent
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

  private fun aggregateByPackage(entries: List<PsProcessEntry>, pm: PackageManager): Map<String, PackageUsage> {
    val map = mutableMapOf<String, PackageUsage>()
    for (entry in entries) {
      val packageName = resolvePackageForEntry(entry, pm) ?: continue
      val current = map[packageName] ?: PackageUsage(0L, 0.0)
      map[packageName] = PackageUsage(
        ramKb = current.ramKb + entry.rssKb,
        cpuPercent = current.cpuPercent + entry.cpuPercent,
      )
    }
    return map
  }

  private fun resolvePackageForEntry(entry: PsProcessEntry, pm: PackageManager): String? {
    val uid = entry.uid ?: androidUserNameToUid(entry.user)
    val packagesForUid = uid?.let {
      if (it < android.os.Process.FIRST_APPLICATION_UID) {
        null
      } else {
        try {
          pm.getPackagesForUid(it.toInt())
        } catch (_: Exception) {
          null
        }
      }
    }
    val byUidPrefix = packagesForUid?.firstOrNull { pkg -> entry.name.startsWith(pkg) }
    val byName = resolvePackageForName(entry.name)
    return when {
      byUidPrefix != null -> byUidPrefix
      byName != null -> byName
      else -> packagesForUid?.firstOrNull()
    }
  }

  private fun resolvePackageForName(name: String): String? {
    val base = name.substringBefore(":")
    if (base in installedPackages) return base
    return installedPackages.firstOrNull { pkg ->
      name.length >= pkg.length + 1 && name.startsWith(pkg) && !name[pkg.length].isLetterOrDigit()
    }
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
