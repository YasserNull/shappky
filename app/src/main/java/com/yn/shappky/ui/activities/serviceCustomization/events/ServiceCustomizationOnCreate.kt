package com.yn.shappky.ui.activities.serviceCustomization.events

import android.os.Bundle
import androidx.activity.compose.setContent
import com.yn.shappky.core.preferences.ShappkyTheme
import com.yn.shappky.core.preferences.applyDynamicColorsFromPreferences
import com.yn.shappky.core.preferences.applySystemBars
import com.yn.shappky.core.preferences.applyThemeFromPreferences
import com.yn.shappky.ui.activities.serviceCustomization.ServiceCustomizationActions
import com.yn.shappky.ui.activities.serviceCustomization.ServiceCustomizationActivity
import com.yn.shappky.ui.activities.serviceCustomization.ServiceCustomizationContent

fun ServiceCustomizationActivity.handleOnCreate(savedInstanceState: Bundle?) {
  applyThemeFromPreferences()
  applyDynamicColorsFromPreferences()
  applySystemBars()

  val initialSettings = ServiceCustomizationActions.loadSettings(this)

  setContent {
    ShappkyTheme {
      ServiceCustomizationContent(
        initialSettings = initialSettings,
        onSave = { ServiceCustomizationActions.saveSettings(this, it) },
        onBack = { finish() },
      )
    }
  }
}
