package com.yassernull.shappky.ui.components

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.yassernull.shappky.core.preferences.KEY_DYNAMIC_COLORS
import com.yassernull.shappky.core.preferences.KEY_FULL_SCREEN
import com.yassernull.shappky.core.preferences.KEY_THEME
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME

fun Activity.applySystemBars() {
  val prefs = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  val appTheme = prefs.getString(KEY_THEME, "dark") ?: "dark"
  val dynamic = prefs.getBoolean(KEY_DYNAMIC_COLORS, false)
  val fullScreen = prefs.getBoolean(KEY_FULL_SCREEN, false)

  val systemBarColor = when (appTheme) {
    "white" -> {
      if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicLightColorScheme(this).surface.toArgb()
      } else {
        0xFFFFFFFF.toInt()
      }
    }
    "black" -> 0xFF000000.toInt()
    else -> {
      if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(this).surface.toArgb()
      } else {
        0xFF17181C.toInt()
      }
    }
  }

  @Suppress("DEPRECATION")
  if (dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    @Suppress("DEPRECATION")
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    @Suppress("DEPRECATION")
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
    window.decorView.setBackgroundColor(systemBarColor)
  } else {
    @Suppress("DEPRECATION")
    window.statusBarColor = systemBarColor
    @Suppress("DEPRECATION")
    window.navigationBarColor = systemBarColor
  }
  WindowCompat.setDecorFitsSystemWindows(window, !fullScreen)
  val controller = WindowInsetsControllerCompat(window, window.decorView)

  val isLight = appTheme == "white"
  controller.isAppearanceLightStatusBars = isLight
  controller.isAppearanceLightNavigationBars = isLight

  if (fullScreen) {
    controller.hide(WindowInsetsCompat.Type.statusBars())
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
  } else {
    controller.show(WindowInsetsCompat.Type.statusBars())
  }
}
