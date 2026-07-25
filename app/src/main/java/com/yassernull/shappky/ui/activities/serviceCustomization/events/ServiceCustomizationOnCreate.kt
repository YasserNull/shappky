package com.yassernull.shappky.ui.activities.serviceCustomization.events

import android.os.Bundle
import androidx.activity.compose.setContent
import com.yassernull.shappky.core.preferences.applyDynamicColorsFromPreferences
import com.yassernull.shappky.core.preferences.applyThemeFromPreferences
import com.yassernull.shappky.ui.activities.serviceCustomization.ServiceCustomizationActions
import com.yassernull.shappky.ui.activities.serviceCustomization.ServiceCustomizationActivity
import com.yassernull.shappky.ui.activities.serviceCustomization.ServiceCustomizationContent
import com.yassernull.shappky.ui.components.applySystemBars
import com.yassernull.shappky.ui.theme.AppTheme

fun ServiceCustomizationActivity.handleOnCreate(savedInstanceState: Bundle?) {
  applyThemeFromPreferences()
  applyDynamicColorsFromPreferences()
  applySystemBars()

  val initialSettings = ServiceCustomizationActions.loadSettings(this)

  setContent {
    AppTheme {
      ServiceCustomizationContent(
        initialSettings = initialSettings,
        onSave = { ServiceCustomizationActions.saveSettings(this, it) },
        onBack = { finish() },
      )
    }
  }
}
