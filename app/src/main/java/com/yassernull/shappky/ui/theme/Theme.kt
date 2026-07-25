package com.yassernull.shappky.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.yassernull.shappky.core.preferences.KEY_DYNAMIC_COLORS
import com.yassernull.shappky.core.preferences.KEY_THEME
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME

@Suppress("DEPRECATION")
@Composable
fun AppTheme(withBackground: Boolean = true, content: @Composable () -> Unit) {
  val context = LocalContext.current
  val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  val appTheme = sharedPreferences.getString(KEY_THEME, "dark") ?: "dark"
  val dynamicColors = sharedPreferences.getBoolean(KEY_DYNAMIC_COLORS, false)

  val colorScheme = when {
    dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      when (appTheme) {
        "white" -> dynamicLightColorScheme(context)
        "black" -> dynamicDarkColorScheme(context).copy(
          surface = Black,
          background = Black,
          surfaceContainer = Black,
          surfaceContainerHigh = Black,
          surfaceContainerHighest = Black,
          surfaceContainerLow = Black,
          surfaceContainerLowest = Black,
        )
        else -> dynamicDarkColorScheme(context)
      }
    }
    else -> {
      when (appTheme) {
        "white" -> lightColorScheme(
          primary = Blue500,
          onPrimary = White,
          surface = White,
          background = White,
          onSurface = LightOnSurface,
        )
        "black" -> darkColorScheme(
          primary = Blue500,
          onPrimary = White,
          surface = Black,
          background = Black,
          onSurface = DarkOnSurface,
        )
        else -> darkColorScheme(
          primary = Blue500,
          onPrimary = White,
          surface = DarkSurface,
          background = DarkSurface,
          onSurface = DarkOnSurface,
        )
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme) {
    if (withBackground) {
      Surface(color = MaterialTheme.colorScheme.surface, content = content)
    } else {
      content()
    }
  }
}
