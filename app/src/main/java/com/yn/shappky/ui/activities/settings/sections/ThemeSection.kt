package com.yn.shappky.ui.activities.settings

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
import com.yn.shappky.R
import com.yn.shappky.ui.components.SettingsHeader
import com.yn.shappky.ui.components.SwitchSettingRow
import com.yn.shappky.ui.components.ValueSettingRow
import com.yn.shappky.ui.dialogs.RestartDialog
import com.yn.shappky.ui.dialogs.ThemeDialog
import com.yn.shappky.utils.getThemeLabel
import com.yn.shappky.utils.restartApp
import com.yn.shappky.utils.updateAllWidgets

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

  if (showThemeDialog) {
    ThemeDialog(
      themeValue = themeValue,
      options = options,
      onThemeSelected = { newTheme ->
        if (newTheme != themeValue) {
          themeValue = newTheme
          sharedPreferences.edit().putString("appTheme", newTheme).apply()
          context.updateAllWidgets()
          if (context is Activity) {
            context.recreate()
          }
        }
        showThemeDialog = false
      },
      onDismiss = { showThemeDialog = false },
    )
  }

  if (showRestartDialog) {
    RestartDialog(
      onRestart = { context.restartApp() },
      onDismiss = { showRestartDialog = false },
    )
  }
}
