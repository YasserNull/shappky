package com.yassernull.shappky.ui.activities.settings.events

import android.os.Bundle
import androidx.activity.compose.setContent
import com.yassernull.shappky.core.preferences.applyDynamicColorsFromPreferences
import com.yassernull.shappky.core.preferences.applyThemeFromPreferences
import com.yassernull.shappky.ui.activities.settings.SettingsActivity
import com.yassernull.shappky.ui.activities.settings.SettingsContent
import com.yassernull.shappky.ui.components.applySystemBars

fun SettingsActivity.handleOnCreate(savedInstanceState: Bundle?) {
  applyThemeFromPreferences()
  applyDynamicColorsFromPreferences()
  applySystemBars()

  setContent {
    com.yassernull.shappky.ui.theme.AppTheme {
      SettingsContent(
        onBack = { finish() },
      )
    }
  }
}
