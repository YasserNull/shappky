package com.yn.shappky.ui.activities.settings

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R
import com.yn.shappky.core.preferences.PREFERENCES_NAME
import com.yn.shappky.core.preferences.RamUsageBarPreferences
import com.yn.shappky.ui.components.ActionSettingRow
import com.yn.shappky.ui.components.SettingsHeader
import com.yn.shappky.ui.dialogs.RefreshIntervalDialog
import com.yn.shappky.ui.dialogs.formatInterval

@Composable
fun RamUsageSection() {
  val context = LocalContext.current
  val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

  var ramUsageBarRefreshIntervalMs by remember {
    mutableStateOf(sharedPreferences.getLong(RamUsageBarPreferences.KEY_REFRESH_INTERVAL_MS, RamUsageBarPreferences.DEFAULT_REFRESH_INTERVAL_MS).coerceAtLeast(500L))
  }
  var showRamUsageBarRefreshIntervalDialog by remember { mutableStateOf(false) }

  SettingsHeader(text = stringResource(R.string.settings_ram_usage_bar))
  ActionSettingRow(
    icon = Icons.Filled.Refresh,
    title = stringResource(R.string.ram_usage_bar_refresh_ms),
    summary = formatInterval(ramUsageBarRefreshIntervalMs),
    onClick = { showRamUsageBarRefreshIntervalDialog = true },
  )

  if (showRamUsageBarRefreshIntervalDialog) {
    RefreshIntervalDialog(
      title = stringResource(R.string.ram_usage_bar_refresh_interval_title),
      currentIntervalMs = ramUsageBarRefreshIntervalMs,
      choices = listOf(500L, 1000L, 2000L, 5000L),
      onApply = { newInterval ->
        val coerced = newInterval.coerceAtLeast(500L)
        ramUsageBarRefreshIntervalMs = coerced
        sharedPreferences.edit().putLong(RamUsageBarPreferences.KEY_REFRESH_INTERVAL_MS, coerced).apply()
        showRamUsageBarRefreshIntervalDialog = false
      },
      onDismiss = { showRamUsageBarRefreshIntervalDialog = false },
    )
  }
}
