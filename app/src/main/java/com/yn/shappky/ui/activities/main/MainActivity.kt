package com.yn.shappky.ui.activities.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.DynamicColors
import com.yn.shappky.R
import com.yn.shappky.ui.activities.SettingsActivity
import com.yn.shappky.model.AppModel
import com.yn.shappky.util.BackgroundAppManager
import com.yn.shappky.util.RamMonitor
import com.yn.shappky.util.RamState
import com.yn.shappky.util.ShellManager
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private lateinit var sharedpreferences: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private lateinit var shellManager: ShellManager
    private lateinit var appManager: BackgroundAppManager
    private lateinit var ramMonitor: RamMonitor
    private val appsDataList = mutableStateListOf<AppModel>()
    private var hasPermission by mutableStateOf(false)
    private var isLoadingBackgroundApps by mutableStateOf(false)
    private var ramState by mutableStateOf(RamState())
    private var showSystemApps by mutableStateOf(false)
    private var showPersistentApps by mutableStateOf(false)
    private var appsAutoRefresh = false
    private var appsRamUsageAutoRefresh = false
    private var appsAutoRefreshIntervalMs = DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS
    private var appsRamUsageRefreshIntervalMs = DEFAULT_APPS_RAM_USAGE_REFRESH_INTERVAL_MS
    private var ramUsageBarRefreshIntervalMs = DEFAULT_RAM_USAGE_BAR_REFRESH_INTERVAL_MS
    private var currentTheme = "dark"
    private var currentDynamicColors = false
    private var backgroundLoadRetryCount = 0
    private var appsAutoRefreshRunnable: Runnable? = null
    private var appsRamUsageRunnable: Runnable? = null

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            Log.d(
                TAG,
                "Shizuku permission result received requestCode=$requestCode, grantResult=$grantResult, granted=${grantResult == PackageManager.PERMISSION_GRANTED}",
            )
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Shizuku permission granted from listener, binding service and loading apps")
                shellManager.bindShizukuService()
                loadBackgroundApps()
            } else {
                Log.w(TAG, "Shizuku permission denied from listener, grantResult=$grantResult")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyPendingFullScreenPreference()
        applyThemeFromPreferences()
        applyDynamicColorsFromPreferences()
        sharedpreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        currentTheme = sharedpreferences.getString(KEY_THEME, "dark") ?: "dark"
        currentDynamicColors = sharedpreferences.getBoolean(KEY_DYNAMIC_COLORS, false)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_CODE,
            )
        }

        shellManager = ShellManager(this, handler, executor)
        appManager = BackgroundAppManager(this, handler, executor, shellManager)
        ramMonitor = RamMonitor(handler) { ramState = it }
        showSystemApps = sharedpreferences.getBoolean(KEY_SHOW_SYSTEM_APPS, false)
        showPersistentApps = sharedpreferences.getBoolean(KEY_SHOW_PERSISTENT_APPS, false)
        appsAutoRefresh = sharedpreferences.getBoolean(KEY_APPS_AUTO_REFRESH, false)
        appsRamUsageAutoRefresh = sharedpreferences.getBoolean(KEY_APPS_RAM_USAGE_AUTO_REFRESH, false)
        appsAutoRefreshIntervalMs = sharedpreferences.getLong(KEY_APPS_AUTO_REFRESH_INTERVAL_MS, DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS)
        appsRamUsageRefreshIntervalMs =
            sharedpreferences.getLong(
                KEY_APPS_RAM_USAGE_REFRESH_INTERVAL_MS,
                DEFAULT_APPS_RAM_USAGE_REFRESH_INTERVAL_MS,
            )
        ramUsageBarRefreshIntervalMs =
            sharedpreferences.getLong(
                KEY_RAM_USAGE_BAR_REFRESH_INTERVAL_MS,
                DEFAULT_RAM_USAGE_BAR_REFRESH_INTERVAL_MS,
            )
        ramMonitor.setRefreshIntervalMs(ramUsageBarRefreshIntervalMs)
        appManager.setShowSystemApps(showSystemApps)
        appManager.setShowPersistentApps(showPersistentApps)

        shellManager.setShizukuPermissionListener(shizukuPermissionListener)
        shellManager.setOnShizukuServiceConnected(
            Runnable {
                Log.d(TAG, "Shizuku service connected callback")
                if (appsDataList.isEmpty()) loadBackgroundApps()
            },
        )
        shellManager.checkShellPermissions()
        updatePermissionUi()
        ramMonitor.startMonitoring()
        updateAppsAutoRefresh()
        updateAppsRamUsageAutoRefresh()
        applySystemBars()

        setContent {
            ShappkyTheme {
                MainScreen(
                    apps = appsDataList,
                    ramState = ramState,
                    hasPermission = hasPermission,
                    isLoadingBackgroundApps = isLoadingBackgroundApps,
                    showSystemApps = showSystemApps,
                    showPersistentApps = showPersistentApps,
                    initialSortMode = sharedpreferences.getString(KEY_SORT_MODE, SORT_BY_NAME) ?: SORT_BY_NAME,
                    initialSortDescending = sharedpreferences.getBoolean(KEY_SORT_DESCENDING, false),
                    sortByName = SORT_BY_NAME,
                    sortByRam = SORT_BY_RAM,
                    hiddenApps = appManager.getHiddenApps(),
                    onSelectAll = { selected ->
                        appsDataList.replaceAllSelection(selected)
                        updateSelectMenuVisibility()
                    },
                    onRefresh = { loadBackgroundApps(showRefreshIndicator = true) },
                    onToggleShowSystemApps = {
                        showSystemApps = !showSystemApps
                        appManager.setShowSystemApps(showSystemApps)
                        sharedpreferences.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, showSystemApps).apply()
                        appsDataList.replaceAllSelection(false)
                        loadBackgroundApps(showRefreshIndicator = true)
                    },
                    onToggleShowPersistentApps = {
                        showPersistentApps = !showPersistentApps
                        appManager.setShowPersistentApps(showPersistentApps)
                        sharedpreferences.edit().putBoolean(KEY_SHOW_PERSISTENT_APPS, showPersistentApps).apply()
                        appsDataList.replaceAllSelection(false)
                        loadBackgroundApps(showRefreshIndicator = true)
                    },
                    onOpenSettings = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    },
                    onOpenDonate = {
                        openUrl(getString(R.string.donate_url))
                    },
                    onKillSelected = { killSelectedApps() },
                    onToggleApp = { app ->
                        if (!app.isProtected) {
                            replaceApp(app.copy(isSelected = !app.isSelected))
                        }
                    },
                    onKillApp = { app ->
                        appManager.killApp(app.packageName, Runnable { loadBackgroundApps(showRefreshIndicator = true) })
                    },
                    onApplySort = { sortMode, descending ->
                        sharedpreferences.edit()
                            .putString(KEY_SORT_MODE, sortMode)
                            .putBoolean(KEY_SORT_DESCENDING, descending)
                            .apply()
                        sortAppsDataList()
                    },
                    onLoadAllApps = { onLoaded ->
                        appManager.loadAllApps { result -> onLoaded(result) }
                    },
                    onSaveHiddenApps = { appManager.saveHiddenApps(it) },
                    onFilterSaved = { loadBackgroundApps(showRefreshIndicator = true) },
                )
            }
        }
    }

    @Composable
    private fun ShappkyTheme(content: @Composable () -> Unit) {
        val surface = Color(resolveThemeColor(com.google.android.material.R.attr.colorSurface))
        val onSurface = Color(resolveThemeColor(com.google.android.material.R.attr.colorOnSurface))
        val primary = Color(resolveThemeColor(com.google.android.material.R.attr.colorPrimary))
        val onPrimary = Color(resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary))
        val colorScheme =
            if ((sharedpreferences.getString(KEY_THEME, "dark") ?: "dark") == "white") {
                lightColorScheme(
                    primary = primary,
                    onPrimary = onPrimary,
                    surface = surface,
                    background = surface,
                    onSurface = onSurface,
                )
            } else {
                darkColorScheme(
                    primary = primary,
                    onPrimary = onPrimary,
                    surface = surface,
                    background = surface,
                    onSurface = onSurface,
                )
            }
        MaterialTheme(colorScheme = colorScheme) {
            Surface(color = MaterialTheme.colorScheme.surface, content = content)
        }
    }

    private fun loadBackgroundApps(showRefreshIndicator: Boolean = true) {
        Log.d(
            TAG,
            "loadBackgroundApps requested showRefreshIndicator=$showRefreshIndicator, isLoading=$isLoadingBackgroundApps, currentListSize=${appsDataList.size}, hasPermission=$hasPermission",
        )
        if (showRefreshIndicator && isLoadingBackgroundApps) {
            Log.d(TAG, "loadBackgroundApps skipped because visible refresh is already loading")
            return
        }
        if (!shellManager.hasAnyShellPermission()) {
            Log.w(TAG, "loadBackgroundApps no shell permission, updating permission UI")
            updatePermissionUi()
            return
        }
        if (showRefreshIndicator) {
            Log.d(TAG, "loadBackgroundApps enabling swipe refresh indicator")
            isLoadingBackgroundApps = true
        } else {
            Log.d(TAG, "loadBackgroundApps silent refresh, swipe indicator stays hidden")
        }
        appManager.loadBackgroundApps { result ->
            Log.d(
                TAG,
                "loadBackgroundApps callback resultSize=${result.size}, oldListSize=${appsDataList.size}, showRefreshIndicator=$showRefreshIndicator",
            )
            if (showRefreshIndicator) {
                Log.d(TAG, "loadBackgroundApps disabling swipe refresh indicator")
                isLoadingBackgroundApps = false
            }
            if (result.isEmpty() && appsDataList.isEmpty() && backgroundLoadRetryCount < 5) {
                backgroundLoadRetryCount++
                Log.w(TAG, "loadBackgroundApps empty result, retry=$backgroundLoadRetryCount scheduled")
                handler.postDelayed({ loadBackgroundApps(showRefreshIndicator) }, 700)
                return@loadBackgroundApps
            }
            backgroundLoadRetryCount = 0
            Log.d(TAG, "loadBackgroundApps replacing list oldSize=${appsDataList.size}, newSize=${result.size}")
            appsDataList.clear()
            appsDataList.addAll(result)
            Log.d(TAG, "loadBackgroundApps sorting list")
            sortAppsDataList()
            Log.d(TAG, "loadBackgroundApps updating select menu visibility")
            updateSelectMenuVisibility()
        }
    }

    private fun killSelectedApps() {
        val packagesToKill = appsDataList.filter { it.isSelected }.map { it.packageName }
        if (packagesToKill.isEmpty()) return
        appsDataList.replaceAllSelection(false)
        appManager.killPackages(
            packagesToKill,
            Runnable {
                loadBackgroundApps()
                updateSelectMenuVisibility()
            },
        )
    }

    private fun updateSelectMenuVisibility() {
        val updated = appsDataList.toList()
        appsDataList.clear()
        appsDataList.addAll(updated)
    }

    private fun MutableList<AppModel>.replaceAllSelection(selected: Boolean) {
        val updated = map { app -> if (!app.isProtected) app.copy(isSelected = selected) else app }
        clear()
        addAll(updated)
    }

    private fun replaceApp(app: AppModel) {
        val index = appsDataList.indexOfFirst { it.packageName == app.packageName }
        if (index >= 0) appsDataList[index] = app.copy()
    }

    private fun sortAppsDataList() {
        val sortMode = sharedpreferences.getString(KEY_SORT_MODE, SORT_BY_NAME)
        val descending = sharedpreferences.getBoolean(KEY_SORT_DESCENDING, false)
        Log.d(TAG, "sortAppsDataList sortMode=$sortMode, descending=$descending, size=${appsDataList.size}")
        val appTypeComparator = compareBy<AppModel> { it.isSystemApp }.thenBy { it.isPersistentApp }
        val comparator =
            if (sortMode == SORT_BY_RAM) {
                val ramComparator =
                    if (descending) compareByDescending<AppModel> { parseMemoryToKb(it.appRam) }
                    else compareBy { parseMemoryToKb(it.appRam) }
                appTypeComparator.then(ramComparator).thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName }
            } else {
                val nameComparator =
                    if (descending) compareByDescending<AppModel> { it.appName.lowercase(Locale.getDefault()) }
                    else compareBy { it.appName.lowercase(Locale.getDefault()) }
                appTypeComparator.then(nameComparator)
            }
        val sorted = appsDataList.sortedWith(comparator)
        appsDataList.clear()
        appsDataList.addAll(sorted)
        appsDataList.forEachIndexed { index, app ->
            Log.d(TAG, "sortAppsDataList result[$index] label=${app.appName}, package=${app.packageName}, ram=${app.appRam}")
        }
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
        } catch (_: NumberFormatException) {
            0
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
        }
    }

    private fun updateAppsAutoRefresh() {
        if (appsAutoRefresh) {
            startAppsAutoRefresh()
        } else {
            stopAppsAutoRefresh()
        }
    }

    private fun startAppsAutoRefresh() {
        if (appsAutoRefreshRunnable != null) return
        appsAutoRefreshRunnable =
            object : Runnable {
                override fun run() {
                    loadBackgroundApps(showRefreshIndicator = false)
                    handler.postDelayed(this, appsAutoRefreshIntervalMs)
                }
            }
        handler.postDelayed(requireNotNull(appsAutoRefreshRunnable), appsAutoRefreshIntervalMs)
    }

    private fun stopAppsAutoRefresh() {
        appsAutoRefreshRunnable?.let { handler.removeCallbacks(it) }
        appsAutoRefreshRunnable = null
    }

    private fun updateAppsRamUsageAutoRefresh() {
        if (appsRamUsageAutoRefresh) {
            startAppsRamUsageAutoRefresh()
        } else {
            stopAppsRamUsageAutoRefresh()
        }
    }

    private fun startAppsRamUsageAutoRefresh() {
        if (appsRamUsageRunnable != null) return
        appsRamUsageRunnable =
            object : Runnable {
                override fun run() {
                    refreshAppsRamUsage()
                    handler.postDelayed(this, appsRamUsageRefreshIntervalMs)
                }
            }
        handler.postDelayed(requireNotNull(appsRamUsageRunnable), appsRamUsageRefreshIntervalMs)
    }

    private fun stopAppsRamUsageAutoRefresh() {
        appsRamUsageRunnable?.let { handler.removeCallbacks(it) }
        appsRamUsageRunnable = null
    }

    private fun refreshAppsRamUsage() {
        Log.d(TAG, "refreshAppsRamUsage requested hasPermission=$hasPermission, listSize=${appsDataList.size}")
        if (!hasPermission || appsDataList.isEmpty()) {
            Log.d(TAG, "refreshAppsRamUsage skipped hasPermission=$hasPermission, listSize=${appsDataList.size}")
            return
        }
        appManager.loadAppsRamUsage(appsDataList.map { it.packageName }) { ramUsageByPackage ->
            Log.d(TAG, "refreshAppsRamUsage callback mapSize=${ramUsageByPackage.size}, values=$ramUsageByPackage")
            if (ramUsageByPackage.isNotEmpty()) {
                val updatedApps =
                    appsDataList.map { app ->
                        val newRam = ramUsageByPackage[app.packageName] ?: app.appRam
                        Log.d(
                            TAG,
                            "refreshAppsRamUsage app package=${app.packageName}, oldRam=${app.appRam}, newRam=$newRam",
                        )
                        app.copy(appRam = ramUsageByPackage[app.packageName] ?: app.appRam)
                    }
                appsDataList.clear()
                appsDataList.addAll(updatedApps)
                if (sharedpreferences.getString(KEY_SORT_MODE, SORT_BY_NAME) == SORT_BY_RAM) {
                    Log.d(TAG, "refreshAppsRamUsage sort mode is RAM, resorting")
                    sortAppsDataList()
                } else {
                    Log.d(TAG, "refreshAppsRamUsage sort mode is not RAM, keeping current order")
                }
            } else {
                Log.w(TAG, "refreshAppsRamUsage empty RAM usage map")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shellManager.removeShizukuPermissionListener()
        shellManager.unbindShizukuService()
        stopAppsAutoRefresh()
        stopAppsRamUsageAutoRefresh()
        executor.shutdownNow()
        handler.removeCallbacksAndMessages(null)
        ramMonitor.stopMonitoring()
    }

    override fun onResume() {
        super.onResume()
        val themeNow = sharedpreferences.getString(KEY_THEME, "dark")
        val dynamicNow = sharedpreferences.getBoolean(KEY_DYNAMIC_COLORS, false)
        if (themeNow != currentTheme || dynamicNow != currentDynamicColors) {
            recreate()
            return
        }
        applySystemBars()
        updatePermissionUi()
        val appsAutoRefreshNow = sharedpreferences.getBoolean(KEY_APPS_AUTO_REFRESH, false)
        val appsAutoRefreshIntervalNow =
            sharedpreferences.getLong(KEY_APPS_AUTO_REFRESH_INTERVAL_MS, DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS)
        if (appsAutoRefreshNow != appsAutoRefresh || appsAutoRefreshIntervalNow != appsAutoRefreshIntervalMs) {
            appsAutoRefresh = appsAutoRefreshNow
            appsAutoRefreshIntervalMs = appsAutoRefreshIntervalNow
            stopAppsAutoRefresh()
            updateAppsAutoRefresh()
        }
        val appsRamUsageAutoRefreshNow = sharedpreferences.getBoolean(KEY_APPS_RAM_USAGE_AUTO_REFRESH, false)
        val appsRamUsageRefreshIntervalNow =
            sharedpreferences.getLong(
                KEY_APPS_RAM_USAGE_REFRESH_INTERVAL_MS,
                DEFAULT_APPS_RAM_USAGE_REFRESH_INTERVAL_MS,
            )
        if (appsRamUsageAutoRefreshNow != appsRamUsageAutoRefresh) {
            appsRamUsageAutoRefresh = appsRamUsageAutoRefreshNow
            appsRamUsageRefreshIntervalMs = appsRamUsageRefreshIntervalNow
            stopAppsRamUsageAutoRefresh()
            updateAppsRamUsageAutoRefresh()
        } else if (appsRamUsageRefreshIntervalNow != appsRamUsageRefreshIntervalMs) {
            appsRamUsageRefreshIntervalMs = appsRamUsageRefreshIntervalNow
            stopAppsRamUsageAutoRefresh()
            updateAppsRamUsageAutoRefresh()
        }
        val ramUsageBarRefreshIntervalNow =
            sharedpreferences.getLong(
                KEY_RAM_USAGE_BAR_REFRESH_INTERVAL_MS,
                DEFAULT_RAM_USAGE_BAR_REFRESH_INTERVAL_MS,
            )
        if (ramUsageBarRefreshIntervalNow != ramUsageBarRefreshIntervalMs) {
            ramUsageBarRefreshIntervalMs = ramUsageBarRefreshIntervalNow
            ramMonitor.setRefreshIntervalMs(ramUsageBarRefreshIntervalMs)
        }
    }

    private fun applySystemBars() {
        val fullScreen = sharedpreferences.getBoolean(KEY_FULL_SCREEN, false)
        val systemBarColor = resolveThemeColor(com.google.android.material.R.attr.colorSurface)
        window.statusBarColor = systemBarColor
        window.navigationBarColor = systemBarColor
        WindowCompat.setDecorFitsSystemWindows(window, !fullScreen)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (fullScreen) {
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    private fun applyThemeFromPreferences() {
        val prefs = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val theme = prefs.getString(KEY_THEME, "dark")
        val dynamic = prefs.getBoolean(KEY_DYNAMIC_COLORS, false)
        if (dynamic) {
            when (theme) {
                "white" -> setTheme(R.style.AppTheme_Dynamic_Light)
                "black" -> setTheme(R.style.AppTheme_Dynamic_Black)
                else -> setTheme(R.style.AppTheme_Dynamic_Dark)
            }
            return
        }
        when (theme) {
            "white" -> setTheme(R.style.AppTheme_Light)
            "black" -> setTheme(R.style.AppTheme_Black)
            else -> setTheme(R.style.AppTheme_Dark)
        }
    }

    private fun applyDynamicColorsFromPreferences() {
        val prefs = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DYNAMIC_COLORS, false)) {
            DynamicColors.applyToActivityIfAvailable(this)
            if (prefs.getString(KEY_THEME, "dark") == "black") {
                theme.applyStyle(R.style.AppTheme_Dynamic_Black_Override, true)
            }
        }
    }

    private fun applyPendingFullScreenPreference() {
        val prefs = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        if (prefs.contains("fullScreenPending")) {
            val pending = prefs.getBoolean("fullScreenPending", false)
            prefs.edit().putBoolean(KEY_FULL_SCREEN, pending).remove("fullScreenPending").apply()
        }
    }

    private fun resolveThemeColor(attr: Int): Int {
        val value = TypedValue()
        theme.resolveAttribute(attr, value, true)
        return value.data
    }

    private fun updatePermissionUi() {
        val previous = hasPermission
        hasPermission = shellManager.hasAnyShellPermission()
        Log.d(TAG, "updatePermissionUi previous=$previous, current=$hasPermission")
        if (hasPermission) loadBackgroundApps()
    }

    companion object {
        private const val TAG = "ShappkyMain"
        private const val NOTIFICATION_PERMISSION_CODE = 1
        private const val PREFERENCES_NAME = "AppPreferences"
        private const val KEY_SHOW_SYSTEM_APPS = "showSystemApps"
        private const val KEY_SHOW_PERSISTENT_APPS = "showPersistentApps"
        private const val KEY_APPS_AUTO_REFRESH = "appsAutoRefresh"
        private const val KEY_APPS_RAM_USAGE_AUTO_REFRESH = "appsRamUsageAutoRefresh"
        private const val KEY_APPS_AUTO_REFRESH_INTERVAL_MS = "appsAutoRefreshIntervalMs"
        private const val KEY_APPS_RAM_USAGE_REFRESH_INTERVAL_MS = "appsRamUsageRefreshIntervalMs"
        private const val KEY_RAM_USAGE_BAR_REFRESH_INTERVAL_MS = "ramUsageBarRefreshIntervalMs"
        private const val KEY_FULL_SCREEN = "fullScreen"
        private const val KEY_THEME = "appTheme"
        private const val KEY_DYNAMIC_COLORS = "dynamicColors"
        private const val KEY_SORT_MODE = "sortMode"
        private const val KEY_SORT_DESCENDING = "sortDescending"
        private const val DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS = 5000L
        private const val DEFAULT_APPS_RAM_USAGE_REFRESH_INTERVAL_MS = 3000L
        private const val DEFAULT_RAM_USAGE_BAR_REFRESH_INTERVAL_MS = 1000L
        private const val SORT_BY_NAME = "name"
        private const val SORT_BY_RAM = "ram"
    }
}
