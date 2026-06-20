package com.yn.shappky.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.yn.shappky.R
import com.yn.shappky.model.AppModel
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
    private var showSystemApps = false
    private var showPersistentApps = false
    private val sharedpreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var isCurrentlyLoadingApps = false

    @Volatile
    private var isCurrentlyLoadingRam = false

    fun formatMemorySize(kb: Long): String = when {
        kb < 1024 -> context.getString(R.string.kb_format, kb)
        kb < 1024 * 1024 -> context.getString(R.string.mb_format, kb / 1024f)
        else -> context.getString(R.string.gb_format, kb / (1024f * 1024f))
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
        if (isCurrentlyLoadingApps) {
            Log.d(TAG, "loadBackgroundApps skipped because another load is already in progress")
            return
        }
        isCurrentlyLoadingApps = true
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

                var currentKeyboardPackage =
                    Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                if (currentKeyboardPackage != null && currentKeyboardPackage.contains("/")) {
                    currentKeyboardPackage = currentKeyboardPackage.split("/")[0]
                }
                Log.d(TAG, "Current keyboard package=$currentKeyboardPackage")

                val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val resolveInfo = packageManager.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
                val currentLauncherPackage = resolveInfo?.activityInfo?.packageName
                Log.d(TAG, "Current launcher package=$currentLauncherPackage")

                if (shellManager.isShellCommandReady()) {
                    val command = "ps -A -o rss,name | grep '\\.' | grep -v '[-:@]'"
                    try {
                        Log.d(TAG, "Running process command: $command")
                        val fullOutput = shellManager.runShellCommandAndGetFullOutput(command)
                        if (fullOutput != null) {
                            Log.d(TAG, "Process command outputLength=${fullOutput.length}")
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
                                        val packageName = parts[1].trim()
                                        val appRam = parts[0].trim()
                                        Log.d(TAG, "ps line#$lineCount parsed package=$packageName, rssKb=$appRam, parts=${parts.size}")
                                        if (
                                            packageName.isNotEmpty() &&
                                            packageName.contains(".") &&
                                            !packageName.startsWith("ERROR:")
                                        ) {
                                            try {
                                                packageManager.getApplicationInfo(packageName, 0)
                                                runningPackagesFromPs.add("$packageName:$appRam")
                                                parsedCount++
                                                Log.d(
                                                    TAG,
                                                    "ps accepted package=$packageName, rssKb=$appRam, uniqueSoFar=${runningPackagesFromPs.size}",
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

                        val isProtected =
                            packageName == "com.yn.shappky" ||
                                packageName == "com.google.android.gms" ||
                                packageName == "com.android.systemui" ||
                                packageName == "com.android.bluetooth" ||
                                packageName == "com.android.externalstorage" ||
                                packageName == "com.google.android.providers.media.module" ||
                                packageName == "com.miui.miwallpaper" ||
                                packageName == "com.android.camera" ||
                                packageName == currentKeyboardPackage ||
                                packageName == currentLauncherPackage

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
                    callback?.accept(ArrayList(result))
                }
            }
        }
    }

    fun loadAllApps(callback: Consumer<List<AppModel>>) {
        executor.execute {
            val startTime = System.currentTimeMillis()
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            Log.d(TAG, "loadAllApps started installedPackages=${packages.size}")
            val allApps = mutableListOf<AppModel>()
            for (appInfo in packages) {
                if (appInfo.packageName == context.packageName) {
                    Log.d(TAG, "loadAllApps skipping self package=${appInfo.packageName}")
                    continue
                }
                val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                val isPersistent = appInfo.flags and ApplicationInfo.FLAG_PERSISTENT != 0
                val label = pm.getApplicationLabel(appInfo).toString()
                Log.d(
                    TAG,
                    "loadAllApps add label=$label, package=${appInfo.packageName}, system=$isSystem, persistent=$isPersistent",
                )
                allApps.add(
                    AppModel(
                        appName = label,
                        packageName = appInfo.packageName,
                        appRam = "-",
                        ramKb = 0L,
                        appIcon = pm.getApplicationIcon(appInfo),
                        isSystemApp = isSystem,
                        isPersistentApp = isPersistent,
                        isProtected = false,
                    ),
                )
            }
            allApps.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            Log.d(TAG, "loadAllApps finished count=${allApps.size}, durationMs=${System.currentTimeMillis() - startTime}")
            handler.post { callback.accept(allApps) }
        }
    }

    fun loadAppsRamUsage(packageNames: List<String>, callback: Consumer<Map<String, Long>>) {
        if (isCurrentlyLoadingRam) {
            Log.d(TAG, "loadAppsRamUsage skipped because another RAM load is in progress")
            return
        }
        isCurrentlyLoadingRam = true
        executor.execute {
            val startTime = System.currentTimeMillis()
            val requestedPackages = packageNames.toSet()
            val ramUsageByPackage = mutableMapOf<String, Long>()
            try {
                Log.d(TAG, "loadAppsRamUsage started requestedCount=${requestedPackages.size}, requested=$requestedPackages")
                if (requestedPackages.isNotEmpty() && shellManager.isShellCommandReady()) {
                    val command = "ps -A -o rss,name | grep '\\.' | grep -v '[-:@]'"
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
                                        val packageName = parts[1].trim()
                                        if (packageName in requestedPackages) {
                                            matchedCount++
                                            ramUsageByPackage[packageName] =
                                                (ramUsageByPackage[packageName] ?: 0L) + ramUsage
                                            Log.d(
                                                TAG,
                                                "loadAppsRamUsage matched package=$packageName, ramKb=$ramUsage, totalKb=${ramUsageByPackage[packageName]}",
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

    fun getHiddenApps(): Set<String> = sharedpreferences.getStringSet(KEY_HIDDEN_APPS, HashSet()) ?: HashSet()

    fun saveHiddenApps(hiddenApps: Set<String>) {
        sharedpreferences.edit().putStringSet(KEY_HIDDEN_APPS, hiddenApps).apply()
    }

    fun killPackages(packageNames: List<String>?, onComplete: Runnable?) {
        if (!shellManager.hasAnyShellPermission()) {
            shellManager.checkShellPermissions()
            onComplete?.let { handler.post(it) }
            return
        }

        if (packageNames.isNullOrEmpty()) {
            onComplete?.let { handler.post(it) }
            return
        }

        var totalKb = 0L
        for (pkg in packageNames) {
            currentAppsList.firstOrNull { it.packageName == pkg }?.let {
                totalKb += it.ramKb
            }
        }

        val command = packageNames.joinToString("; ") { "am force-stop $it" }
        shellManager.runShellCommand(command, onComplete)
        val message = context.getString(R.string.free_up_memory, formatMemorySize(totalKb))
        handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }

    fun killApp(packageName: String?, onComplete: Runnable?) {
        if (!shellManager.hasAnyShellPermission()) {
            shellManager.checkShellPermissions()
            onComplete?.let { handler.post(it) }
            return
        }
        if (packageName.isNullOrEmpty()) {
            onComplete?.let { handler.post(it) }
            return
        }
        shellManager.runShellCommand("am force-stop $packageName", onComplete)
        currentAppsList.firstOrNull { it.packageName == packageName }?.let { app ->
            val message = context.getString(R.string.free_up_memory, formatMemorySize(app.ramKb))
            handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
        }
    }

    fun setShowSystemApps(show: Boolean) {
        showSystemApps = show
    }

    fun setShowPersistentApps(show: Boolean) {
        showPersistentApps = show
    }

    fun getAppsList(): List<AppModel> = ArrayList(currentAppsList)

    companion object {
        private const val TAG = "ShappkyApps"
        private const val PREFERENCES_NAME = "AppPreferences"
        private const val KEY_HIDDEN_APPS = "hidden_apps"
    }
}
