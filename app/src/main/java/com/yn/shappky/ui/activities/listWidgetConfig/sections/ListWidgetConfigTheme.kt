package com.yn.shappky.ui.activities.listWidgetConfig.sections

import android.content.Context
import androidx.compose.foundation.layout.*
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
import com.yn.shappky.core.preferences.WidgetPreferences
import com.yn.shappky.ui.activities.listWidgetConfig.ListWidgetConfigThemeDialogs
import com.yn.shappky.ui.components.ColorPickerRow
import com.yn.shappky.ui.components.RowSetting
import com.yn.shappky.ui.components.SectionHeader

@Composable
fun ListWidgetConfigTheme(appWidgetId: Int) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }

  var bgColor by remember {
    mutableStateOf(prefs.getInt(WidgetPreferences.getListBgColorKey(appWidgetId), 0xFF0088FF.toInt()))
  }
  var autoBg by remember {
    mutableStateOf(prefs.getBoolean(WidgetPreferences.getListAutoBgKey(appWidgetId), true))
  }
  var showColorPicker by remember { mutableStateOf(false) }

  SectionHeader(stringResource(R.string.theme))
  RowSetting(stringResource(R.string.widget_list_auto_bg), autoBg) {
    autoBg = it
    prefs.edit().putBoolean(WidgetPreferences.getListAutoBgKey(appWidgetId), it).apply()
  }

  if (!autoBg) {
    ColorPickerRow(
      title = stringResource(R.string.widget_bg_color),
      color = bgColor,
      onClick = { showColorPicker = true },
    )
  }

  ListWidgetConfigThemeDialogs(
    showColorPicker = showColorPicker,
    onDismissColorPicker = { showColorPicker = false },
    onColorSelected = {
      bgColor = it
      prefs.edit().putInt(WidgetPreferences.getListBgColorKey(appWidgetId), it).apply()
      showColorPicker = false
    },
    bgColor = bgColor,
  )

  HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}
