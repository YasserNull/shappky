package com.yassernull.shappky.ui.activities.widgetConfig.events

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import com.yassernull.shappky.ui.activities.widgetConfig.WidgetConfigActivity
import com.yassernull.shappky.ui.activities.widgetConfig.WidgetConfigScreen
import com.yassernull.shappky.ui.theme.AppTheme

fun WidgetConfigActivity.handleOnCreate(savedInstanceState: Bundle?) {
  com.yassernull.shappky.utils.applyWidgetThemeFromPreferences(this)
  com.yassernull.shappky.utils.applyWidgetDynamicColorsFromPreferences(this)
  setResult(Activity.RESULT_CANCELED)
  Log.d("WidgetConfig", "WidgetConfigActivity onCreate - initially setting RESULT_CANCELED")

  window.setBackgroundDrawableResource(android.R.color.transparent)
  window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
  window.setDimAmount(0.6f)

  appWidgetId = intent?.extras?.getInt(
    AppWidgetManager.EXTRA_APPWIDGET_ID,
    AppWidgetManager.INVALID_APPWIDGET_ID,
  ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

  Log.d("WidgetConfig", "WidgetConfigActivity Received appWidgetId: \$appWidgetId")

  if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
    Log.d("WidgetConfig", "Invalid appWidgetId, finishing!")
    finish()
    return
  }

  setContent {
    AppTheme {
      WidgetConfigScreen(
        appWidgetId = appWidgetId,
        onSave = {
          Log.d("WidgetConfig", "WidgetConfigActivity onSave called for appWidgetId \$appWidgetId")
          val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
          }
          setResult(Activity.RESULT_OK, resultValue)
          finish()
        },
        onDismiss = {
          Log.d("WidgetConfig", "WidgetConfigActivity onDismiss called (canceled)")
          finish()
        },
      )
    }
  }
}
