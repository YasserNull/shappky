package com.yassernull.shappky.ui.activities.widgetConfig.segments

import androidx.compose.runtime.Composable
import com.yassernull.shappky.ui.components.ColorPickerDialog

@Composable
fun WidgetConfigDialogs(
  showIconColorPicker: Boolean,
  onDismissIconColorPicker: () -> Unit,
  selectedIconColor: Int,
  onIconColorChange: (Int) -> Unit,
  showBgColorPicker: Boolean,
  onDismissBgColorPicker: () -> Unit,
  selectedBgColor: Int,
  onBgColorChange: (Int) -> Unit,
) {
  if (showIconColorPicker) {
    ColorPickerDialog(
      initialColor = selectedIconColor,
      showAlpha = false,
      onDismiss = onDismissIconColorPicker,
      onColorSelected = { color ->
        onIconColorChange(color)
        onDismissIconColorPicker()
      },
    )
  }

  if (showBgColorPicker) {
    ColorPickerDialog(
      initialColor = selectedBgColor,
      showAlpha = true,
      onDismiss = onDismissBgColorPicker,
      onColorSelected = { color ->
        onBgColorChange(color)
        onDismissBgColorPicker()
      },
    )
  }
}
