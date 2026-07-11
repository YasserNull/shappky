package com.yn.shappky.ui.activities.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.yn.shappky.ui.activities.settings.events.handleOnCreate

class SettingsActivity : ComponentActivity() {

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(com.yn.shappky.utils.LanguageHelper.getLanguageContext(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleOnCreate(savedInstanceState)
  }

  companion object {
    internal const val PREFERENCES_NAME = "AppPreferences"
  }
}
