package com.yn.shappky.ui.activities.widgetConfig

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.yn.shappky.ui.activities.widgetConfig.events.handleOnCreate

class WidgetConfigActivity : ComponentActivity() {
  internal var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(com.yn.shappky.utils.LanguageHelper.getLanguageContext(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleOnCreate(savedInstanceState)
  }
}
