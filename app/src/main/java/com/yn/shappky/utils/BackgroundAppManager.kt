package com.yn.shappky.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.yn.shappky.R
import com.yn.shappky.data.models.AppModel
import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.function.Consumer

class BackgroundAppManager(
  private val context: Context,
  private val handler: Handler,
  private val executor: ExecutorService,
  private val shellManager: ShellManager,
) {
  private val currentAppsList = mutableListOf<AppModel>()
  private var showUserApps = true
  private var showSystemApps = false
  private var showPersistentApps = false
  private var showProtectedApps = true
  private val sharedpreferences =
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

  private fun parseMemoryToKb(ram: String?): Long {
    if (ram.isNullOrEmpty() || ram == "-") return 0
    val normalizedRam = ram.trim().uppercase(Locale.getDefault())
    return try {
      when {
        normalizedRam.endsWith("KB") -> normalizedRam.replace("KB", "").trim().toFloat().toLong()
        normalizedRam.endsWith("MB") -> (normalizedRam.replace("MB", "").trim().toFloat() * 1024).toLong()
        normalizedRam.endsWith("GB") -> (normalizedRam.replace("GB", "").trim().toFloat() * 1024 * 1024).toLong()
        else -> 0
      }
    } catch (e: NumberFormatException) {
      e.printStackTrace()
      0
    }
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
        val result = mutableListOf<AppModel>()
        try {
          val packageManager = context.packageManager
          val runningPackagesFromPs = mutableSetOf<String>()
          val hiddenApps = getHiddenApps()
          Log.d(TAG, "Hidden apps loaded count=${hiddenApps.size}, values=$hiddenApps")

          val protectedApps = com.yn.shappky.utils.ProtectionManager.getProtectedApps(context)
          val activeWidgetPackages = getActiveWidgetPackages()
          Log.d(TAG, "Found active widget packages: $activeWidgetPackages")

          if (shellManager.isShellCommandReady()) {
            val command = "${ShellManager.TOYBOX_PATH} ps -A -o rss,name | grep '\\.' | grep -v '[-@]'"
            try {
              Log.d(TAG, "Running process command: $command")
              val fullOutput = shellManager.runShellCommandAndGetFullOutput(command)
              if (fullOutput != null) {
                Log.d(TAG, "Process command outputLength=${fullOutput.length}")
                val packageRamMap = mutableMapOf<String, Long>()
                BufferedReader(StringReader(fullOutput)).use { reader ->
                  var line = reader.readLine()
                  var lineCount = 0
                  var parsedCount = 0
                  var ignoredShortLineCount = 0
                  var ignoredInvalidPackageCount = 0
                  var missingPackageCount = 0
                  while (line != null) {
                    lineCount++
                    Log.d(TAG, "ps line#$lineCount raw=$line")
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 2) {
                      val rawPackageName = parts[1].trim()
                      val packageName = if (rawPackageName.contains(":")) rawPackageName.substringBefore(":") else rawPackageName
                      val appRam = parts[0].trim()
                      val appRamLong = appRam.toLongOrNull() ?: 0L
                      Log.d(TAG, "ps line#$lineCount parsed package=$packageName (raw=$rawPackageName), rssKb=$appRamLong, parts=${parts.size}")
                      if (
                        packageName.isNotEmpty() &&
                        packageName.contains(".") &&
                        !packageName.startsWith("ERROR:")
                      ) {
                        try {
                          packageManager.getApplicationInfo(packageName, 0)
                          packageRamMap[packageName] = (packageRamMap[packageName] ?: 0L) + appRamLong
                          parsedCount++
                          Log.d(
                            TAG,
                            "ps accepted package=$packageName, rssKb=$appRamLong, aggregated=${packageRamMap[packageName]}",
                          )
                        } catch (_: PackageManager.NameNotFoundException) {
                          missingPackageCount++
                          Log.d(TAG, "ps package missing from PackageManager package=$packageName")
                        }
                      } else {
                        ignoredInvalidPackageCount++
                        Log.d(TAG, "ps ignored invalid package line#$lineCount package=$packageName")
                      }
                    } else {
                      ignoredShortLineCount++
                      Log.d(TAG, "ps ignored short line#$lineCount parts=${parts.size}")
                    }
                    line = reader.readLine()
                  }
                  for ((pkg, ram) in packageRamMap) {
                    runningPackagesFromPs.add("$pkg:$ram")
                  }
                  Log.d(
                    TAG,
                    "Process output lines=$lineCount, parsedPackages=$parsedCount, uniquePackages=${runningPackagesFromPs.size}, ignoredShort=$ignoredShortLineCount, ignoredInvalid=$ignoredInvalidPackageCount, missingPackages=$missingPackageCount",
                  )
                }
              } else {
                Log.w(TAG, "Process command returned null output")
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

          var hiddenSkippedCount = 0
          var systemSkippedCount = 0
          var persistentSkippedCount = 0
          var missingDuringBuildCount = 0
          Log.d(TAG, "Building AppModel list from runningPackages size=${runningPackagesFromPs.size}")
          for (packageEntry in runningPackagesFromPs) {
            val parts = packageEntry.split(":")
            val packageName = parts[0]
            val ramUsage = parts.getOrNull(1)?.toLongOrNull() ?: 0
            Log.d(TAG, "Evaluating running package package=$packageName, ramKb=$ramUsage, rawEntry=$packageEntry")

            try {
              if (hiddenApps.contains(packageName)) {
                hiddenSkippedCount++
                Log.d(TAG, "Skipping hidden app package=$packageName")
                continue
              }

              val isProtected = protectedApps.contains(packageName) ||
                com.yn.shappky.utils.ProtectionManager.isAppProtectedByRegex(context, packageName)

              val appInfo = packageManager.getApplicationInfo(packageName, 0)
              val isPersistentApp = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
              val isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
              val label = packageManager.getApplicationLabel(appInfo).toString()

              if (!showSystemApps && isSystemApp) {
                systemSkippedCount++
                Log.d(TAG, "Skipping system app package=$packageName, label=$label")
                continue
              }
              if (!showPersistentApps && isPersistentApp) {
                persistentSkippedCount++
                Log.d(TAG, "Skipping persistent app package=$packageName, label=$label")
                continue
              }
              if (!showProtectedApps && isProtected) {
                Log.d(TAG, "Skipping protected app package=$packageName, label=$label")
                continue
              }
              if (!showUserApps && !isSystemApp && !isPersistentApp) {
                Log.d(TAG, "Skipping user app package=$packageName, label=$label")
                continue
              }

              result.add(
                AppModel(
                  appName = label,
                  packageName = packageName,
                  appRam = formatMemorySize(ramUsage),
                  ramKb = ramUsage,
                  appIcon = packageManager.getApplicationIcon(appInfo),
                  isSystemApp = isSystemApp,
                  isPersistentApp = isPersistentApp,
                  isProtected = isProtected,
                ),
              )
              Log.d(
                TAG,
                "Added running app label=$label, package=$packageName, ram=${formatMemorySize(ramUsage)}, system=$isSystemApp, persistent=$isPersistentApp, protected=$isProtected",
              )
            } catch (_: PackageManager.NameNotFoundException) {
              missingDuringBuildCount++
              Log.d(TAG, "Package disappeared while building app list package=$packageName")
            } catch (e: Exception) {
              Log.e(TAG, "Error processing application info for package=$packageName", e)
            }
          }
          Log.d(
            TAG,
            "Pre-sort app list size=${result.size}, hiddenSkipped=$hiddenSkippedCount, systemSkipped=$systemSkippedCount, persistentSkipped=$persistentSkippedCount, missingDuringBuild=$missingDuringBuildCount",
          )
          result.sortWith(
            compareBy<AppModel> { it.isSystemApp }
              .thenBy { it.isPersistentApp }
              .thenBy { it.appName.lowercase(Locale.getDefault()) },
          )
          result.forEachIndexed { index, app ->
            Log.d(
              TAG,
              "Sorted app[$index] label=${app.appName}, package=${app.packageName}, ram=${app.appRam}, system=${app.isSystemApp}, persistent=${app.isPersistentApp}, protected=${app.isProtected}",
            )
          }
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
        val pm = context.packageManager
        val protectedApps = com.yn.shappky.utils.ProtectionManager.getProtectedApps(context)
        val activeWidgetPackages = getActiveWidgetPackages()
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        Log.d(TAG, "loadAllApps started installedPackages=${packages.size}")
        val allApps = mutableListOf<AppModel>()
        for (appInfo in packages) {
          val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
          val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
          val label = pm.getApplicationLabel(appInfo).toString()
          val pkg = appInfo.packageName
          val isProtected = protectedApps.contains(pkg) ||
            com.yn.shappky.utils.ProtectionManager.isAppProtectedByRegex(context, pkg)

          Log.d(
            TAG,
            "loadAllApps add label=$label, package=$pkg, system=$isSystem, persistent=$isPersistent, protected=$isProtected",
          )
          allApps.add(
            AppModel(
              appName = label,
              packageName = pkg,
              appRam = "-",
              ramKb = 0L,
              appIcon = pm.getApplicationIcon(appInfo),
              isSystemApp = isSystem,
              isPersistentApp = isPersistent,
              isProtected = isProtected,
            ),
          )
        }
        allApps.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
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
          Log.d(TAG, "loadAppsRamUsage started requestedCount=${requestedPackages.size}, requested=$requestedPackages")
          if (requestedPackages.isNotEmpty() && shellManager.isShellCommandReady()) {
            val command = "${ShellManager.TOYBOX_PATH} ps -A -o rss,name | grep '\\.' | grep -v '[-@]'"
            try {
              Log.d(TAG, "loadAppsRamUsage running command=$command")
              val fullOutput = shellManager.runShellCommandAndGetFullOutput(command)
              if (fullOutput != null) {
                Log.d(TAG, "loadAppsRamUsage command outputLength=${fullOutput.length}")
                BufferedReader(StringReader(fullOutput)).use { reader ->
                  var line = reader.readLine()
                  var lineCount = 0
                  var matchedCount = 0
                  while (line != null) {
                    lineCount++
                    Log.d(TAG, "loadAppsRamUsage ps line#$lineCount raw=$line")
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 2) {
                      val ramUsage = parts[0].trim().toLongOrNull() ?: 0L
                      val rawPackageName = parts[1].trim()
                      val packageName = if (rawPackageName.contains(":")) rawPackageName.substringBefore(":") else rawPackageName
                      if (packageName in requestedPackages) {
                        matchedCount++
                        ramUsageByPackage[packageName] =
                          (ramUsageByPackage[packageName] ?: 0L) + ramUsage
                        Log.d(
                          TAG,
                          "loadAppsRamUsage matched package=$packageName (raw=$rawPackageName), ramKb=$ramUsage, totalKb=${ramUsageByPackage[packageName]}",
                        )
                      } else {
                        Log.d(TAG, "loadAppsRamUsage ignored package=$packageName not requested")
                      }
                    } else {
                      Log.d(TAG, "loadAppsRamUsage ignored short line#$lineCount parts=${parts.size}")
                    }
                    line = reader.readLine()
                  }
                  Log.d(TAG, "loadAppsRamUsage parsed lines=$lineCount, matched=$matchedCount")
                }
              } else {
                Log.w(TAG, "loadAppsRamUsage command returned null output")
              }
            } catch (e: Exception) {
              Log.e(TAG, "Error updating app RAM usage", e)
            }
          } else {
            Log.w(
              TAG,
              "loadAppsRamUsage skipped requestedEmpty=${requestedPackages.isEmpty()}, shellReady=${shellManager.isShellCommandReady()}",
            )
          }
          Log.d(
            TAG,
            "loadAppsRamUsage finished count=${ramUsageByPackage.size}, values=$ramUsageByPackage, durationMs=${System.currentTimeMillis() - startTime}",
          )
          handler.post { callback.accept(ramUsageByPackage) }
        } catch (t: Throwable) {
          Log.e(TAG, "Fatal error in loadAppsRamUsage background thread", t)
        } finally {
          isCurrentlyLoadingRam = false
        }
      }
    }
  }

  fun loadAppDetailedInfo(app: AppModel, callback: Consumer<com.yn.shappky.data.models.AppDetailedInfo>) {
    if (!executor.isShutdown) {
      executor.execute {
        try {
          if (shellManager.isShellCommandReady()) {
            val command = "${ShellManager.TOYBOX_PATH} ps -A -o pid,user,rss,name | grep '\\.' | grep -v '[-@]' | grep '" + app.packageName + "'"
            val fullOutput = shellManager.runShellCommandAndGetFullOutput(command)
            val processes = mutableListOf<com.yn.shappky.data.models.ProcessInfo>()
            var mainPid = "-"
            var mainUser = "-"
            var totalCpu = 0.0
            var totalThreads = 0
            var totalRam = 0L
            var isForeground = false

            if (fullOutput != null) {
              java.io.BufferedReader(java.io.StringReader(fullOutput)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                  val parts = line.trim().split(Regex("\\s+"))
                  if (parts.size >= 4 && !line.startsWith("ERROR:")) {
                    val pid = parts[0]
                    val user = parts[1]
                    val rss = parts[2].toLongOrNull() ?: 0L
                    val name = parts[3]

                    if (name.startsWith(app.packageName)) {
                      processes.add(com.yn.shappky.data.models.ProcessInfo(name, pid, rss))
                      if (name == app.packageName) {
                        mainPid = pid
                        mainUser = user
                      }
                      totalRam += rss

                      // Try to get thread count from /proc
                      try {
                        val statOutput = shellManager.runShellCommandAndGetFullOutput("cat /proc/$pid/stat")
                        if (statOutput != null && !statOutput.startsWith("ERROR")) {
                          val statParts = statOutput.trim().split(" ")
                          if (statParts.size >= 20) {
                            totalThreads += statParts[19].toIntOrNull() ?: 0
                          }
                        }
                      } catch (e: Exception) {}
                    }
                  }
                  line = reader.readLine()
                }
              }
            }

            if (mainPid == "-" && processes.isNotEmpty()) {
              mainPid = processes[0].pid
              mainUser = "N/A"
            }

            // Fetch CPU usage
            try {
              val cpuOutput = shellManager.runShellCommandAndGetFullOutput("dumpsys cpuinfo | grep " + app.packageName)
              if (cpuOutput != null && !cpuOutput.startsWith("ERROR")) {
                java.io.BufferedReader(java.io.StringReader(cpuOutput)).use { reader ->
                  var line = reader.readLine()
                  while (line != null) {
                    val parts = line.trim().split(Regex("\\s+"))
                    // Example line: "3.8% 30495/com.google.android.youtube: 3% user + 0.8% kernel"
                    if (parts.isNotEmpty() && parts[0].endsWith("%")) {
                      val percentStr = parts[0].removeSuffix("%")
                      totalCpu += percentStr.toDoubleOrNull() ?: 0.0
                    }
                    line = reader.readLine()
                  }
                }
              }
            } catch (e: Exception) {}

            val dumpCommand = "dumpsys activity services " + app.packageName + " | grep isForeground=true"
            val dumpOutput = shellManager.runShellCommandAndGetFullOutput(dumpCommand)
            isForeground = !dumpOutput.isNullOrBlank() && !dumpOutput.startsWith("ERROR")

            val result = com.yn.shappky.data.models.AppDetailedInfo(
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

  fun getHiddenApps(): Set<String> = sharedpreferences.getStringSet(KEY_HIDDEN_APPS, HashSet()) ?: HashSet()

  fun saveHiddenApps(hiddenApps: Set<String>) {
    sharedpreferences.edit().putStringSet(KEY_HIDDEN_APPS, hiddenApps).apply()
  }

  fun killPackages(packageNames: List<String>?, onComplete: Runnable?, showToast: Boolean = true, appendKillAll: Boolean = false) {
    if (!shellManager.hasAnyShellPermission()) {
      shellManager.checkShellPermissions()
      onComplete?.let { handler.post(it) }
      return
    }

    if (packageNames.isNullOrEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }

    val protectedApps = ProtectionManager.getProtectedApps(context)
    val safePackageNames = packageNames.filter { !protectedApps.contains(it) }

    if (safePackageNames.isEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }

    var totalKb = 0L
    for (pkg in safePackageNames) {
      currentAppsList.firstOrNull { it.packageName == pkg }?.let {
        totalKb += it.ramKb
      }
    }

    val command = buildSmartKillCommand(safePackageNames, appendKillAll)
    shellManager.runShellCommand(command, onComplete)
    if (showToast) {
      val message = context.getString(R.string.free_up_memory, formatMemorySize(totalKb))
      handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }
  }

  fun killApp(packageName: String?, onComplete: Runnable?, forceKill: Boolean = false, appendKillAll: Boolean = false) {
    if (!shellManager.hasAnyShellPermission()) {
      shellManager.checkShellPermissions()
      onComplete?.let { handler.post(it) }
      return
    }
    if (packageName.isNullOrEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }

    if (!forceKill) {
      val protectedApps = ProtectionManager.getProtectedApps(context)
      if (protectedApps.contains(packageName) || ProtectionManager.isAppProtectedByRegex(context, packageName)) {
        onComplete?.let { handler.post(it) }
        return
      }
    }

    val command = buildSmartKillCommand(listOf(packageName), appendKillAll)
    shellManager.runShellCommand(command, onComplete)
    currentAppsList.firstOrNull { it.packageName == packageName }?.let { app ->
      val message = context.getString(R.string.free_up_memory, formatMemorySize(app.ramKb))
      handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }
  }

  fun setShowUserApps(show: Boolean) {
    showUserApps = show
  }

  fun setShowSystemApps(show: Boolean) {
    showSystemApps = show
  }

  fun setShowPersistentApps(show: Boolean) {
    showPersistentApps = show
  }

  fun setShowProtectedApps(show: Boolean) {
    showProtectedApps = show
  }

  fun getAppsList(): List<AppModel> = ArrayList(currentAppsList)

  companion object {
    fun buildSmartKillCommand(packageNames: List<String>, appendKillAll: Boolean = false): String {
      if (packageNames.isEmpty()) return ""
      val killCommands = packageNames.joinToString("; ") { "am kill " + it }
      val forceStopCommands = packageNames.joinToString("; ") { "if pidof " + it + " > /dev/null; then am force-stop " + it + "; fi" }
      val kill9Commands = packageNames.joinToString("; ") {
        "pids=${'$'}(${ShellManager.TOYBOX_PATH} ps -A -o pid,name | grep '" + it + "' | grep -v '[-@]' | awk '{print ${'$'}1}'); if [ ! -z \"${'$'}pids\" ]; then kill -9 ${'$'}pids 2>/dev/null; fi"
      }
      val baseCommand = killCommands + "; sleep 0.2; " + forceStopCommands + "; sleep 0.2; " + kill9Commands
      return if (appendKillAll) baseCommand + "; am kill-all" else baseCommand
    }

    private const val TAG = "ShappkyApps"
    private const val PREFERENCES_NAME = "AppPreferences"
    private const val KEY_HIDDEN_APPS = "hidden_apps"
  }
}
