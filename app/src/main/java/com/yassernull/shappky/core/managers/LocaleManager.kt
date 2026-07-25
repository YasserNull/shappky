package com.yassernull.shappky.core.managers

import android.content.Context
import com.yassernull.shappky.core.preferences.KEY_LANGUAGE
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME

object LocaleManager {
  fun getLanguageContext(newBase: Context): Context {
    val prefs = newBase.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val language = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
    return if (language != "system") {
      val locale = java.util.Locale.forLanguageTag(language)
      java.util.Locale.setDefault(locale)
      val config = android.content.res.Configuration(newBase.resources.configuration)
      config.setLocale(locale)
      newBase.createConfigurationContext(config)
    } else {
      newBase
    }
  }
}
