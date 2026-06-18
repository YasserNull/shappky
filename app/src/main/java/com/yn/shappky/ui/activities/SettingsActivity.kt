package com.yn.shappky.ui.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.DynamicColors
import com.yn.shappky.R

class SettingsActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private var showRestartDialog by mutableStateOf(false)
    private var showThemeDialog by mutableStateOf(false)
    private var showPermissionDialog by mutableStateOf(false)
    private var showAppsAutoRefreshIntervalDialog by mutableStateOf(false)
    private var showAppsRamUsageRefreshIntervalDialog by mutableStateOf(false)
    private var showRamUsageBarRefreshIntervalDialog by mutableStateOf(false)
    private var showSystemApps by mutableStateOf(false)
    private var showPersistentApps by mutableStateOf(false)
    private var appsAutoRefresh by mutableStateOf(false)
    private var appsRamUsageAutoRefresh by mutableStateOf(false)
    private var appsAutoRefreshIntervalMs by mutableStateOf(DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS)
    private var appsRamUsageRefreshIntervalMs by mutableStateOf(DEFAULT_APPS_RAM_USAGE_REFRESH_INTERVAL_MS)
    private var ramUsageBarRefreshIntervalMs by mutableStateOf(DEFAULT_RAM_USAGE_BAR_REFRESH_INTERVAL_MS)
    private var fullScreen by mutableStateOf(false)
    private var dynamicColors by mutableStateOf(false)
    private var themeValue by mutableStateOf("dark")
    private var permissionMode by mutableStateOf("shizuku")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyThemeFromPreferences()
        applyDynamicColorsFromPreferences()
        sharedPreferences = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        readPreferences()
        applySystemBars()
        setContent {
            ShappkyTheme {
                SettingsScreen(
                    showSystemApps = showSystemApps,
                    showPersistentApps = showPersistentApps,
                    appsAutoRefresh = appsAutoRefresh,
                    appsRamUsageAutoRefresh = appsRamUsageAutoRefresh,
                    appsAutoRefreshIntervalMs = appsAutoRefreshIntervalMs,
                    appsRamUsageRefreshIntervalMs = appsRamUsageRefreshIntervalMs,
                    ramUsageBarRefreshIntervalMs = ramUsageBarRefreshIntervalMs,
                    defaultAppsAutoRefreshIntervalMs = DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS,
                    defaultAppsRamUsageRefreshIntervalMs = DEFAULT_APPS_RAM_USAGE_REFRESH_INTERVAL_MS,
                    defaultRamUsageBarRefreshIntervalMs = DEFAULT_RAM_USAGE_BAR_REFRESH_INTERVAL_MS,
                    fullScreen = fullScreen,
                    dynamicColors = dynamicColors,
                    themeValue = themeValue,
                    permissionMode = permissionMode,
                    showRestartDialog = showRestartDialog,
                    showThemeDialog = showThemeDialog,
                    showPermissionDialog = showPermissionDialog,
                    showAppsAutoRefreshIntervalDialog = showAppsAutoRefreshIntervalDialog,
                    showAppsRamUsageRefreshIntervalDialog = showAppsRamUsageRefreshIntervalDialog,
                    showRamUsageBarRefreshIntervalDialog = showRamUsageBarRefreshIntervalDialog,
                    onBack = { finish() },
                    onShowSystemAppsChange = {
                        showSystemApps = it
                        sharedPreferences.edit().putBoolean(KEY_SHOW_SYSTEM_APPS, it).apply()
                    },
                    onShowPersistentAppsChange = {
                        showPersistentApps = it
                        sharedPreferences.edit().putBoolean(KEY_SHOW_PERSISTENT_APPS, it).apply()
                    },
                    onAppsAutoRefreshChange = {
                        appsAutoRefresh = it
                        sharedPreferences.edit().putBoolean(KEY_APPS_AUTO_REFRESH, it).apply()
                        if (it) showAppsAutoRefreshIntervalDialog = true
                    },
                    onAppsRamUsageAutoRefreshChange = {
                        appsRamUsageAutoRefresh = it
                        sharedPreferences.edit().putBoolean(KEY_APPS_RAM_USAGE_AUTO_REFRESH, it).apply()
                        if (it) showAppsRamUsageRefreshIntervalDialog = true
                    },
                    onFullScreenChange = {
                        fullScreen = it
                        sharedPreferences.edit().putBoolean(KEY_FULLSCREEN_PENDING, it).apply()
                        showRestartDialog = true
                    },
                    onDynamicColorsChange = {
                        dynamicColors = it
                        sharedPreferences.edit().putBoolean(KEY_DYNAMIC_COLORS, it).apply()
                        recreate()
                    },
                    onThemeSelected = {
                        if (it != themeValue) {
                            themeValue = it
                            sharedPreferences.edit().putString(KEY_THEME, it).apply()
                            recreate()
                        }
                        showThemeDialog = false
                    },
                    onPermissionModeSelected = {
                        permissionMode = it
                        sharedPreferences.edit().putString(KEY_PERMISSION_MODE, it).apply()
                        showPermissionDialog = false
                    },
                    onRestart = { restartApp() },
                    onDismissRestart = { showRestartDialog = false },
                    onShowThemeDialog = { showThemeDialog = true },
                    onDismissThemeDialog = { showThemeDialog = false },
                    onShowPermissionDialog = { showPermissionDialog = true },
                    onDismissPermissionDialog = { showPermissionDialog = false },
                    onShowAppsAutoRefreshIntervalDialog = { showAppsAutoRefreshIntervalDialog = true },
                    onDismissAppsAutoRefreshIntervalDialog = { showAppsAutoRefreshIntervalDialog = false },
                    onApplyAppsAutoRefreshInterval = {
                        appsAutoRefreshIntervalMs = it
                        sharedPreferences.edit().putLong(KEY_APPS_AUTO_REFRESH_INTERVAL_MS, it).apply()
                        showAppsAutoRefreshIntervalDialog = false
                    },
                    onShowAppsRamUsageRefreshIntervalDialog = { showAppsRamUsageRefreshIntervalDialog = true },
                    onDismissAppsRamUsageRefreshIntervalDialog = {
                        showAppsRamUsageRefreshIntervalDialog = false
                    },
                    onApplyAppsRamUsageRefreshInterval = {
                        appsRamUsageRefreshIntervalMs = it
                        sharedPreferences.edit().putLong(KEY_APPS_RAM_USAGE_REFRESH_INTERVAL_MS, it).apply()
                        showAppsRamUsageRefreshIntervalDialog = false
                    },
                    onShowRamUsageBarRefreshIntervalDialog = { showRamUsageBarRefreshIntervalDialog = true },
                    onDismissRamUsageBarRefreshIntervalDialog = {
                        showRamUsageBarRefreshIntervalDialog = false
                    },
                    onApplyRamUsageBarRefreshInterval = {
                        ramUsageBarRefreshIntervalMs = it
                        sharedPreferences.edit().putLong(KEY_RAM_USAGE_BAR_REFRESH_INTERVAL_MS, it).apply()
                        showRamUsageBarRefreshIntervalDialog = false
                    },
                    onOpenDonate = {
                        openUrl(getString(R.string.donate_url))
                    },
                    onOpenSourceCode = {
                        openUrl(getString(R.string.source_code_url))
                    },
                )
            }
        }
    }

    private fun readPreferences() {
        showSystemApps = sharedPreferences.getBoolean(KEY_SHOW_SYSTEM_APPS, false)
        showPersistentApps = sharedPreferences.getBoolean(KEY_SHOW_PERSISTENT_APPS, false)
        appsAutoRefresh = sharedPreferences.getBoolean(KEY_APPS_AUTO_REFRESH, false)
        appsRamUsageAutoRefresh = sharedPreferences.getBoolean(KEY_APPS_RAM_USAGE_AUTO_REFRESH, false)
        appsAutoRefreshIntervalMs =
            sharedPreferences.getLong(KEY_APPS_AUTO_REFRESH_INTERVAL_MS, DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS)
        appsRamUsageRefreshIntervalMs =
            sharedPreferences.getLong(
                KEY_APPS_RAM_USAGE_REFRESH_INTERVAL_MS,
                DEFAULT_APPS_RAM_USAGE_REFRESH_INTERVAL_MS,
            )
        ramUsageBarRefreshIntervalMs =
            sharedPreferences.getLong(
                KEY_RAM_USAGE_BAR_REFRESH_INTERVAL_MS,
                DEFAULT_RAM_USAGE_BAR_REFRESH_INTERVAL_MS,
            )
        fullScreen = sharedPreferences.getBoolean(KEY_FULL_SCREEN, false)
        dynamicColors = sharedPreferences.getBoolean(KEY_DYNAMIC_COLORS, false)
        themeValue = sharedPreferences.getString(KEY_THEME, "dark") ?: "dark"
        permissionMode = sharedPreferences.getString(KEY_PERMISSION_MODE, "shizuku") ?: "shizuku"
    }

    @Composable
    private fun ShappkyTheme(content: @Composable () -> Unit) {
        val surface = Color(resolveThemeColor(com.google.android.material.R.attr.colorSurface))
        val onSurface = Color(resolveThemeColor(com.google.android.material.R.attr.colorOnSurface))
        val primary = Color(resolveThemeColor(com.google.android.material.R.attr.colorPrimary))
        val onPrimary = Color(resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary))
        val colorScheme =
            if (themeValue == "white") {
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

    private fun restartApp() {
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        finish()
        Runtime.getRuntime().exit(0)
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
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

    private fun resolveThemeColor(attr: Int): Int {
        val value = TypedValue()
        theme.resolveAttribute(attr, value, true)
        return value.data
    }

    private fun applySystemBars() {
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

    companion object {
        private const val PREFERENCES_NAME = "AppPreferences"
        private const val KEY_SHOW_SYSTEM_APPS = "showSystemApps"
        private const val KEY_SHOW_PERSISTENT_APPS = "showPersistentApps"
        private const val KEY_APPS_AUTO_REFRESH = "appsAutoRefresh"
        private const val KEY_APPS_RAM_USAGE_AUTO_REFRESH = "appsRamUsageAutoRefresh"
        private const val KEY_APPS_AUTO_REFRESH_INTERVAL_MS = "appsAutoRefreshIntervalMs"
        private const val KEY_APPS_RAM_USAGE_REFRESH_INTERVAL_MS = "appsRamUsageRefreshIntervalMs"
        private const val KEY_RAM_USAGE_BAR_REFRESH_INTERVAL_MS = "ramUsageBarRefreshIntervalMs"
        private const val DEFAULT_APPS_AUTO_REFRESH_INTERVAL_MS = 5000L
        private const val DEFAULT_APPS_RAM_USAGE_REFRESH_INTERVAL_MS = 3000L
        private const val DEFAULT_RAM_USAGE_BAR_REFRESH_INTERVAL_MS = 1000L
        private const val KEY_FULL_SCREEN = "fullScreen"
        private const val KEY_THEME = "appTheme"
        private const val KEY_DYNAMIC_COLORS = "dynamicColors"
        private const val KEY_PERMISSION_MODE = "permissionMode"
        private const val KEY_FULLSCREEN_PENDING = "fullScreenPending"
    }
}
