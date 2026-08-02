package com.yassernull.shappky.utils
import android.app.Activity
import android.content.Context
import com.yassernull.shappky.core.preferences.KEY_FULL_SCREEN
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
fun getThemeIndex(theme: String?): Int = when (theme) {
  "white" -> 1
  "black" -> 2
  else -> 0
}

fun themeFromIndex(index: Int): String = when (index) {
  1 -> "white"
  2 -> "black"
  else -> "dark"
}

fun getThemeLabel(theme: String?, options: Array<String>): String {
  val index = getThemeIndex(theme)
  return if (index in options.indices) options[index] else options[0]
}

fun Activity.applyPendingFullScreenPreference() {
  val prefs = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  if (prefs.contains("fullScreenPending")) {
    val pending = prefs.getBoolean("fullScreenPending", false)
    prefs.edit().putBoolean(KEY_FULL_SCREEN, pending).remove("fullScreenPending").apply()
    recreate()
  }
}

fun applyWidgetThemeFromPreferences(activity: Activity) {
  val prefs = activity.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
  val theme = prefs.getString("appTheme", "dark")
  val dynamic = prefs.getBoolean("dynamicColors", false)
  val themeId = if (dynamic) {
    when (theme) {
      "white" -> com.yassernull.shappky.R.style.Theme_WidgetConfig_Dynamic_Light
      "black" -> com.yassernull.shappky.R.style.Theme_WidgetConfig_Dynamic_Black
      else -> com.yassernull.shappky.R.style.Theme_WidgetConfig_Dynamic_Dark
    }
  } else {
    when (theme) {
      "white" -> com.yassernull.shappky.R.style.Theme_WidgetConfig_Light
      "black" -> com.yassernull.shappky.R.style.Theme_WidgetConfig_Black
      else -> com.yassernull.shappky.R.style.Theme_WidgetConfig_Dark
    }
  }
  activity.setTheme(themeId)
}

fun applyWidgetDynamicColorsFromPreferences(activity: Activity) {
  if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
  val prefs = activity.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
  if (prefs.getBoolean("dynamicColors", false)) {
    com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(activity)
    if (prefs.getString("appTheme", "dark") == "black") {
      activity.theme.applyStyle(com.yassernull.shappky.R.style.AppTheme_Dynamic_Black_Override, true)
    }
  }
}
