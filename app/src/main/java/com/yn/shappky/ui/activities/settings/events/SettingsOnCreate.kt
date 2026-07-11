package com.yn.shappky.ui.activities.settings.events

import android.os.Bundle
import androidx.activity.compose.setContent
import com.yn.shappky.core.preferences.applyDynamicColorsFromPreferences
import com.yn.shappky.core.preferences.applySystemBars
import com.yn.shappky.core.preferences.applyThemeFromPreferences
import com.yn.shappky.ui.activities.settings.SettingsActivity
import com.yn.shappky.ui.activities.settings.SettingsContent

fun SettingsActivity.handleOnCreate(savedInstanceState: Bundle?) {
  applyThemeFromPreferences()
  applyDynamicColorsFromPreferences()
  applySystemBars()

  setContent {
    com.yn.shappky.core.preferences.ShappkyTheme {
      SettingsContent(
        onBack = { finish() },
      )
    }
  }
}
