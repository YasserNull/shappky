package com.yn.shappky.ui.activities

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R
import com.yn.shappky.ui.components.ActionSettingRow
import com.yn.shappky.ui.components.SettingsDivider
import com.yn.shappky.ui.components.SwitchSettingRow
import com.yn.shappky.ui.components.ValueSettingRow
import com.yn.shappky.ui.dialogs.LanguageDialog
import com.yn.shappky.ui.dialogs.PermissionModeDialog
import com.yn.shappky.ui.dialogs.RefreshIntervalDialog
import com.yn.shappky.ui.dialogs.RestartDialog
import com.yn.shappky.ui.dialogs.ThemeDialog
import com.yn.shappky.utils.getLanguageLabel
import com.yn.shappky.utils.getThemeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    showSystemApps: Boolean,
    showPersistentApps: Boolean,
    showAppTypeIcons: Boolean,
    appsAutoRefresh: Boolean,
    appsRamUsageAutoRefresh: Boolean,
    appsAutoRefreshIntervalMs: Long,
    appsRamUsageRefreshIntervalMs: Long,
    ramUsageBarRefreshIntervalMs: Long,
    defaultAppsAutoRefreshIntervalMs: Long,
    defaultAppsRamUsageRefreshIntervalMs: Long,
    defaultRamUsageBarRefreshIntervalMs: Long,
    fullScreen: Boolean,
    dynamicColors: Boolean,
    themeValue: String,
    permissionMode: String,
    languageValue: String,
    showRestartDialog: Boolean,
    showThemeDialog: Boolean,
    showPermissionDialog: Boolean,
    showLanguageDialog: Boolean,
    showAppsAutoRefreshIntervalDialog: Boolean,
    showAppsRamUsageRefreshIntervalDialog: Boolean,
    showRamUsageBarRefreshIntervalDialog: Boolean,
    onBack: () -> Unit,
    onShowSystemAppsChange: (Boolean) -> Unit,
    onShowPersistentAppsChange: (Boolean) -> Unit,
    onShowAppTypeIconsChange: (Boolean) -> Unit,
    onAppsAutoRefreshChange: (Boolean) -> Unit,
    onAppsRamUsageAutoRefreshChange: (Boolean) -> Unit,
    onFullScreenChange: (Boolean) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onThemeSelected: (String) -> Unit,
    onPermissionModeSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onRestart: () -> Unit,
    onDismissRestart: () -> Unit,
    onShowThemeDialog: () -> Unit,
    onDismissThemeDialog: () -> Unit,
    onShowPermissionDialog: () -> Unit,
    onDismissPermissionDialog: () -> Unit,
    onShowLanguageDialog: () -> Unit,
    onDismissLanguageDialog: () -> Unit,
    onShowAppsAutoRefreshIntervalDialog: () -> Unit,
    onDismissAppsAutoRefreshIntervalDialog: () -> Unit,
    onApplyAppsAutoRefreshInterval: (Long) -> Unit,
    onShowAppsRamUsageRefreshIntervalDialog: () -> Unit,
    onDismissAppsRamUsageRefreshIntervalDialog: () -> Unit,
    onApplyAppsRamUsageRefreshInterval: (Long) -> Unit,
    onShowRamUsageBarRefreshIntervalDialog: () -> Unit,
    onDismissRamUsageBarRefreshIntervalDialog: () -> Unit,
    onApplyRamUsageBarRefreshInterval: (Long) -> Unit,
    onOpenDonate: () -> Unit,
    onOpenSourceCode: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SwitchSettingRow(
                icon = Icons.Filled.Apps,
                title = stringResource(R.string.show_system_apps),
                summary = stringResource(R.string.show_system_apps_summary),
                checked = showSystemApps,
                onCheckedChange = onShowSystemAppsChange,
            )
            SettingsDivider()
            SwitchSettingRow(
                icon = Icons.Filled.Apps,
                title = stringResource(R.string.show_persistent_apps),
                summary = stringResource(R.string.show_persistent_apps_summary),
                checked = showPersistentApps,
                onCheckedChange = onShowPersistentAppsChange,
            )
            SettingsDivider()
            SwitchSettingRow(
                icon = Icons.Filled.Apps,
                title = stringResource(R.string.show_app_type_icons),
                summary = stringResource(R.string.show_app_type_icons_summary),
                checked = showAppTypeIcons,
                onCheckedChange = onShowAppTypeIconsChange,
            )
            SettingsDivider()
            SwitchSettingRow(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.apps_auto_refresh),
                summary = stringResource(R.string.refresh_interval_value, appsAutoRefreshIntervalMs),
                checked = appsAutoRefresh,
                onCheckedChange = onAppsAutoRefreshChange,
                onClick = onShowAppsAutoRefreshIntervalDialog,
            )
            SettingsDivider()
            SwitchSettingRow(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.apps_ram_usage_auto_refresh),
                summary = stringResource(R.string.refresh_interval_value, appsRamUsageRefreshIntervalMs),
                checked = appsRamUsageAutoRefresh,
                onCheckedChange = onAppsRamUsageAutoRefreshChange,
                onClick = onShowAppsRamUsageRefreshIntervalDialog,
            )
            SettingsDivider()
            ActionSettingRow(
                icon = Icons.Filled.Refresh,
                title = stringResource(R.string.ram_usage_bar_refresh_ms),
                summary = stringResource(R.string.refresh_interval_value, ramUsageBarRefreshIntervalMs),
                onClick = onShowRamUsageBarRefreshIntervalDialog,
            )
            SettingsDivider()
            SwitchSettingRow(
                icon = Icons.Filled.Fullscreen,
                title = stringResource(R.string.full_screen),
                summary = stringResource(R.string.full_screen_summary),
                checked = fullScreen,
                onCheckedChange = onFullScreenChange,
            )
            SettingsDivider()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchSettingRow(
                    icon = Icons.Filled.ColorLens,
                    title = stringResource(R.string.dynamic_colors),
                    summary = stringResource(R.string.dynamic_colors_summary),
                    checked = dynamicColors,
                    onCheckedChange = onDynamicColorsChange,
                )
                SettingsDivider()
            }
            ValueSettingRow(
                icon = Icons.Filled.Security,
                title = stringResource(R.string.permission_mode),
                summary = stringResource(R.string.permission_mode_summary),
                value = if (permissionMode == "root") {
                    stringResource(R.string.permission_mode_root)
                } else {
                    stringResource(R.string.permission_mode_shizuku)
                },
                onClick = onShowPermissionDialog,
            )
            SettingsDivider()
            val options = stringArrayResource(R.array.theme_options)
            ValueSettingRow(
                icon = Icons.Filled.Palette,
                title = stringResource(R.string.theme),
                summary = stringResource(R.string.theme_summary),
                value = getThemeLabel(themeValue, options),
                onClick = onShowThemeDialog,
            )
            SettingsDivider()
            val languageOptions = stringArrayResource(R.array.language_options)
            ValueSettingRow(
                icon = Icons.Filled.Translate,
                title = stringResource(R.string.language),
                summary = stringResource(R.string.language_summary),
                value = getLanguageLabel(languageValue, languageOptions),
                onClick = onShowLanguageDialog,
            )
            SettingsDivider()
            ActionSettingRow(
                icon = Icons.Filled.Favorite,
                title = stringResource(R.string.donate),
                summary = stringResource(R.string.donate_summary),
                onClick = onOpenDonate,
            )
            SettingsDivider()
            ActionSettingRow(
                icon = Icons.Filled.Code,
                title = stringResource(R.string.source_code),
                summary = stringResource(R.string.source_code_summary),
                onClick = onOpenSourceCode,
            )
            SettingsDivider()
        }
    }

    if (showRestartDialog) {
        RestartDialog(onRestart = onRestart, onDismiss = onDismissRestart)
    }
    if (showLanguageDialog) {
        LanguageDialog(
            languageValue = languageValue,
            options = stringArrayResource(R.array.language_options),
            onLanguageSelected = onLanguageSelected,
            onDismiss = onDismissLanguageDialog,
        )
    }
    if (showThemeDialog) {
        ThemeDialog(
            themeValue = themeValue,
            options = stringArrayResource(R.array.theme_options),
            onThemeSelected = onThemeSelected,
            onDismiss = onDismissThemeDialog,
        )
    }
    if (showPermissionDialog) {
        PermissionModeDialog(
            permissionMode = permissionMode,
            onModeSelected = onPermissionModeSelected,
            onDismiss = onDismissPermissionDialog,
        )
    }
    if (showAppsAutoRefreshIntervalDialog) {
        RefreshIntervalDialog(
            title = stringResource(R.string.apps_auto_refresh_interval_title),
            currentIntervalMs = appsAutoRefreshIntervalMs,
            defaultIntervalMs = defaultAppsAutoRefreshIntervalMs,
            minIntervalMs = 1000L,
            onApply = onApplyAppsAutoRefreshInterval,
            onDismiss = onDismissAppsAutoRefreshIntervalDialog,
        )
    }
    if (showAppsRamUsageRefreshIntervalDialog) {
        RefreshIntervalDialog(
            title = stringResource(R.string.apps_ram_usage_auto_refresh_interval_title),
            currentIntervalMs = appsRamUsageRefreshIntervalMs,
            defaultIntervalMs = defaultAppsRamUsageRefreshIntervalMs,
            minIntervalMs = 1000L,
            onApply = onApplyAppsRamUsageRefreshInterval,
            onDismiss = onDismissAppsRamUsageRefreshIntervalDialog,
        )
    }
    if (showRamUsageBarRefreshIntervalDialog) {
        RefreshIntervalDialog(
            title = stringResource(R.string.ram_usage_bar_refresh_interval_title),
            currentIntervalMs = ramUsageBarRefreshIntervalMs,
            defaultIntervalMs = defaultRamUsageBarRefreshIntervalMs,
            minIntervalMs = 500L,
            onApply = onApplyRamUsageBarRefreshInterval,
            onDismiss = onDismissRamUsageBarRefreshIntervalDialog,
        )
    }
}
