package com.yassernull.shappky.ui.activities.settings

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R
import com.yassernull.shappky.ui.components.SettingsHeader
import com.yassernull.shappky.ui.components.SwitchSettingRow
import com.yassernull.shappky.ui.components.ValueSettingRow
import com.yassernull.shappky.utils.getThemeLabel
import com.yassernull.shappky.utils.restartApp
import com.yassernull.shappky.utils.updateAllWidgets

@Composable
fun ThemeSection() {
  val context = LocalContext.current
  val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

  var themeValue by remember { mutableStateOf(sharedPreferences.getString("appTheme", "dark") ?: "dark") }
  var dynamicColors by remember { mutableStateOf(sharedPreferences.getBoolean("dynamicColors", false)) }
  var fullScreen by remember { mutableStateOf(sharedPreferences.getBoolean("fullScreen", false)) }

  var showThemeDialog by remember { mutableStateOf(false) }
  var showRestartDialog by remember { mutableStateOf(false) }

  SettingsHeader(text = stringResource(R.string.theme))
  val options = stringArrayResource(R.array.theme_options)

  ValueSettingRow(
    icon = Icons.Filled.Palette,
    title = stringResource(R.string.theme),
    summary = stringResource(R.string.theme_summary),
    value = getThemeLabel(themeValue, options),
    onClick = { showThemeDialog = true },
  )
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    SwitchSettingRow(
      icon = Icons.Filled.ColorLens,
      title = stringResource(R.string.dynamic_colors),
      summary = stringResource(R.string.dynamic_colors_summary),
      checked = dynamicColors,
      onCheckedChange = { isChecked ->
        dynamicColors = isChecked
        sharedPreferences.edit().putBoolean("dynamicColors", isChecked).apply()
        context.updateAllWidgets()
        if (context is Activity) {
          context.recreate()
        }
      },
    )
  }
  SwitchSettingRow(
    icon = Icons.Filled.Fullscreen,
    title = stringResource(R.string.full_screen),
    summary = stringResource(R.string.full_screen_summary),
    checked = fullScreen,
    onCheckedChange = { isChecked ->
      fullScreen = isChecked
      sharedPreferences.edit().putBoolean("fullScreenPending", isChecked).apply()
      showRestartDialog = true
    },
  )

  ThemeSettingsDialogs(
    themeValue = themeValue,
    options = options,
    showThemeDialog = showThemeDialog,
    showRestartDialog = showRestartDialog,
    onThemeSelected = { newTheme ->
      themeValue = newTheme
      sharedPreferences.edit().putString("appTheme", newTheme).apply()
      context.updateAllWidgets()
      if (context is Activity) {
        context.recreate()
      }
      showThemeDialog = false
    },
    onRestart = { context.restartApp() },
    onDismissTheme = { showThemeDialog = false },
    onDismissRestart = { showRestartDialog = false },
  )
}
