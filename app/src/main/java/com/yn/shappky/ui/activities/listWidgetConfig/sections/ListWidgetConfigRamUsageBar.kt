package com.yn.shappky.ui.activities.listWidgetConfig.sections

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yn.shappky.R
import com.yn.shappky.core.preferences.RamUsageBarPreferences
import com.yn.shappky.core.preferences.WidgetPreferences
import com.yn.shappky.ui.activities.listWidgetConfig.ListWidgetConfigRamUsageBarDialogs
import com.yn.shappky.ui.components.ActionSettingRow
import com.yn.shappky.ui.components.RowSetting
import com.yn.shappky.ui.components.SectionHeader
import com.yn.shappky.ui.dialogs.formatInterval

@Composable
fun ListWidgetConfigRamUsageBar(appWidgetId: Int) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }

  var ramBarRefresh by remember {
    mutableStateOf(prefs.getBoolean(WidgetPreferences.getListRamBarRefreshKey(appWidgetId), true))
  }
  var ramUsageBarRefreshIntervalMs by remember {
    mutableStateOf(prefs.getLong(RamUsageBarPreferences.KEY_REFRESH_INTERVAL_MS, RamUsageBarPreferences.DEFAULT_REFRESH_INTERVAL_MS))
  }
  var showRamUsageBarRefreshIntervalDialog by remember { mutableStateOf(false) }

  SectionHeader(stringResource(R.string.widget_list_ram_bar))
  RowSetting(stringResource(R.string.widget_list_ram_bar), ramBarRefresh) {
    ramBarRefresh = it
    prefs.edit().putBoolean(WidgetPreferences.getListRamBarRefreshKey(appWidgetId), it).apply()
  }

  if (ramBarRefresh) {
    ActionSettingRow(
      label = stringResource(R.string.ram_usage_bar_refresh_ms),
      summary = formatInterval(ramUsageBarRefreshIntervalMs),
      onClick = { showRamUsageBarRefreshIntervalDialog = true },
    )
  }

  ListWidgetConfigRamUsageBarDialogs(
    showRamUsageBarRefreshIntervalDialog = showRamUsageBarRefreshIntervalDialog,
    onDismissRamUsageBarRefreshIntervalDialog = { showRamUsageBarRefreshIntervalDialog = false },
    onApplyRamUsageBarRefreshInterval = {
      ramUsageBarRefreshIntervalMs = it
      prefs.edit().putLong(RamUsageBarPreferences.KEY_REFRESH_INTERVAL_MS, it).apply()
      showRamUsageBarRefreshIntervalDialog = false
    },
    ramUsageBarRefreshIntervalMs = ramUsageBarRefreshIntervalMs,
  )

  HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
