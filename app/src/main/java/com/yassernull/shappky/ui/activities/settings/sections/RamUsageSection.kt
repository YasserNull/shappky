package com.yassernull.shappky.ui.activities.settings

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
import com.yassernull.shappky.R
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.core.preferences.RamUsageBarPreferences
import com.yassernull.shappky.ui.components.ActionSettingRow
import com.yassernull.shappky.ui.components.SettingsHeader
import com.yassernull.shappky.utils.formatInterval

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

  RamUsageSettingsDialog(
    ramUsageBarRefreshIntervalMs = ramUsageBarRefreshIntervalMs,
    showDialog = showRamUsageBarRefreshIntervalDialog,
    onApply = { newInterval ->
      ramUsageBarRefreshIntervalMs = newInterval
      sharedPreferences.edit().putLong(RamUsageBarPreferences.KEY_REFRESH_INTERVAL_MS, newInterval).apply()
      showRamUsageBarRefreshIntervalDialog = false
    },
    onDismiss = { showRamUsageBarRefreshIntervalDialog = false },
  )
}
