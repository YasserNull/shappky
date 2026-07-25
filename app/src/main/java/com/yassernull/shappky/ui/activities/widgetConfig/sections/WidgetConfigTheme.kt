package com.yassernull.shappky.ui.activities.widgetConfig

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import com.yassernull.shappky.ui.activities.widgetConfig.segments.WidgetConfigDialogs
import com.yassernull.shappky.ui.components.ColorPickerRow

@Composable
fun WidgetConfigTheme(
  appWidgetId: Int,
  selectedBgSize: Int,
  onBgSizeChange: (Int) -> Unit,
  selectedIconColor: Int,
  onIconColorChange: (Int) -> Unit,
  selectedBgColor: Int,
  onBgColorChange: (Int) -> Unit,
) {
  var showIconColorPicker by remember { mutableStateOf(false) }
  var showBgColorPicker by remember { mutableStateOf(false) }

  Text(
    text = stringResource(R.string.widget_bg_size, selectedBgSize),
    fontSize = 14.sp,
    color = MaterialTheme.colorScheme.onSurface,
    modifier = Modifier.fillMaxWidth(),
  )
  Spacer(Modifier.height(4.dp))
  Slider(
    value = selectedBgSize.toFloat(),
    onValueChange = { onBgSizeChange(it.toInt()) },
    valueRange = 40f..60f,
    modifier = Modifier.fillMaxWidth(),
  )
  Spacer(Modifier.height(16.dp))

  ColorPickerRow(
    title = stringResource(R.string.widget_icon_color),
    color = selectedIconColor,
    onClick = { showIconColorPicker = true },
  )
  Spacer(Modifier.height(8.dp))

  ColorPickerRow(
    title = stringResource(R.string.widget_bg_color),
    color = selectedBgColor,
    onClick = { showBgColorPicker = true },
  )
  Spacer(Modifier.height(8.dp))

  WidgetConfigDialogs(
    showIconColorPicker = showIconColorPicker,
    onDismissIconColorPicker = { showIconColorPicker = false },
    selectedIconColor = selectedIconColor,
    onIconColorChange = onIconColorChange,
    showBgColorPicker = showBgColorPicker,
    onDismissBgColorPicker = { showBgColorPicker = false },
    selectedBgColor = selectedBgColor,
    onBgColorChange = onBgColorChange,
  )
}
