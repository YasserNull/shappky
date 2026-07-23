package com.yn.shappky.core.preferences

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.TypedValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.yn.shappky.R

const val PREFERENCES_NAME = "AppPreferences"
const val KEY_FULL_SCREEN = "fullScreen"
const val KEY_THEME = "appTheme"
const val KEY_DYNAMIC_COLORS = "dynamicColors"
const val KEY_LANGUAGE = "appLanguage"

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
    else -> { // dark
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

fun Activity.applyThemeFromPreferences() {
  val prefs = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  val theme = prefs.getString(KEY_THEME, "dark")
  val dynamic = prefs.getBoolean(KEY_DYNAMIC_COLORS, false)
  if (dynamic) {
    when (theme) {
      "white" -> setTheme(R.style.AppTheme_Dynamic_Light)
      "black" -> setTheme(R.style.AppTheme_Dynamic_Black)
      else -> setTheme(R.style.AppTheme_Dynamic_Dark)
    }
    return
  }
  when (theme) {
    "white" -> setTheme(R.style.AppTheme_Light)
    "black" -> setTheme(R.style.AppTheme_Black)
    else -> setTheme(R.style.AppTheme_Dark)
  }
}

fun Activity.applyDynamicColorsFromPreferences() {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
  val prefs = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  if (prefs.getBoolean(KEY_DYNAMIC_COLORS, false)) {
    if (prefs.getString(KEY_THEME, "dark") == "black") {
      theme.applyStyle(R.style.AppTheme_Dynamic_Black_Override, true)
    }
  }
}

fun Activity.resolveThemeColor(attr: Int): Int {
  val value = TypedValue()
  theme.resolveAttribute(attr, value, true)
  return if (value.resourceId != 0) {
    getColor(value.resourceId)
  } else {
    value.data
  }
}

@Suppress("DEPRECATION")
@Composable
fun ShappkyTheme(content: @Composable () -> Unit) {
  val context = LocalContext.current
  val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  val appTheme = sharedPreferences.getString(KEY_THEME, "dark") ?: "dark"
  val dynamicColors = sharedPreferences.getBoolean(KEY_DYNAMIC_COLORS, false)

  val colorScheme = when {
    dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      when (appTheme) {
        "white" -> dynamicLightColorScheme(context)
        "black" -> dynamicDarkColorScheme(context).copy(
          surface = Color.Black,
          background = Color.Black,
          surfaceContainer = Color.Black,
          surfaceContainerHigh = Color.Black,
          surfaceContainerHighest = Color.Black,
          surfaceContainerLow = Color.Black,
          surfaceContainerLowest = Color.Black,
        )
        else -> dynamicDarkColorScheme(context)
      }
    }
    else -> {
      when (appTheme) {
        "white" -> lightColorScheme(
          primary = Color(0xFF0136FF),
          onPrimary = Color(0xFFFFFFFF),
          surface = Color(0xFFFFFFFF),
          background = Color(0xFFFFFFFF),
          onSurface = Color(0xFF111111),
        )
        "black" -> darkColorScheme(
          primary = Color(0xFF0136FF),
          onPrimary = Color(0xFFFFFFFF),
          surface = Color(0xFF000000),
          background = Color(0xFF000000),
          onSurface = Color(0xFFECEFF1),
        )
        else -> darkColorScheme(
          primary = Color(0xFF0136FF),
          onPrimary = Color(0xFFFFFFFF),
          surface = Color(0xFF17181C),
          background = Color(0xFF17181C),
          onSurface = Color(0xFFECEFF1),
        )
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme) {
    Surface(color = MaterialTheme.colorScheme.surface, content = content)
  }
}
