package com.yassernull.shappky.core.preferences

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.TypedValue
import com.yassernull.shappky.R

const val PREFERENCES_NAME = "AppPreferences"
const val KEY_FULL_SCREEN = "fullScreen"
const val KEY_THEME = "appTheme"
const val KEY_DYNAMIC_COLORS = "dynamicColors"
const val KEY_LANGUAGE = "appLanguage"

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
