package com.yassernull.shappky.ui.activities.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.yassernull.shappky.ui.activities.settings.events.handleOnCreate

class SettingsActivity : ComponentActivity() {

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(com.yassernull.shappky.core.managers.LocaleManager.getLanguageContext(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleOnCreate(savedInstanceState)
  }

  companion object {
    internal const val PREFERENCES_NAME = "AppPreferences"
  }
}
