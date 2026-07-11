package com.yn.shappky.ui.activities.serviceCustomization

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.yn.shappky.ui.activities.serviceCustomization.events.handleOnCreate

class ServiceCustomizationActivity : ComponentActivity() {
  private lateinit var sharedPreferences: SharedPreferences

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
