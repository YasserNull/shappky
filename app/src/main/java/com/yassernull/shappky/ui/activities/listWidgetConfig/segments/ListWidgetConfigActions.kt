package com.yassernull.shappky.ui.activities.listWidgetConfig

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.util.Log

object ListWidgetConfigActions {
  fun onSave(activity: Activity, appWidgetId: Int) {
    Log.d("WidgetConfig", "ListWidgetConfigActivity onSave called for appWidgetId \$appWidgetId")
    val resultValue = Intent().apply {
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    activity.setResult(Activity.RESULT_OK, resultValue)
    activity.finish()
  }

  fun onDismiss(activity: Activity) {
    Log.d("WidgetConfig", "ListWidgetConfigActivity onDismiss called (canceled)")
    activity.finish()
  }
}
