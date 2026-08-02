package com.yassernull.shappky.ui.activities.listWidgetConfig.events

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.ui.activities.listWidgetConfig.ListWidgetConfigActions
import com.yassernull.shappky.ui.activities.listWidgetConfig.ListWidgetConfigActivity
import com.yassernull.shappky.ui.activities.listWidgetConfig.ListWidgetConfigContent
import com.yassernull.shappky.ui.theme.AppTheme
import androidx.compose.ui.graphics.Color as ComposeColor

fun ListWidgetConfigActivity.handleOnCreate(savedInstanceState: Bundle?) {
  com.yassernull.shappky.utils.applyWidgetThemeFromPreferences(this)
  com.yassernull.shappky.utils.applyWidgetDynamicColorsFromPreferences(this)
  setResult(Activity.RESULT_CANCELED)
  Log.d("WidgetConfig", "ListWidgetConfigActivity onCreate - initially setting RESULT_CANCELED")

  window.setBackgroundDrawableResource(android.R.color.transparent)
  window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
  window.setDimAmount(0.6f)

  appWidgetId = intent?.extras?.getInt(
    AppWidgetManager.EXTRA_APPWIDGET_ID,
    AppWidgetManager.INVALID_APPWIDGET_ID,
  ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

  Log.d("WidgetConfig", "ListWidgetConfigActivity Received appWidgetId: $appWidgetId")

  if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
    Log.d("WidgetConfig", "Invalid appWidgetId, finishing!")
    finish()
    return
  }

  val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
  val appTheme = prefs.getString("appTheme", "dark") ?: "dark"

  setContent {
    AppTheme(withBackground = false) {
      val dialogBg = if (appTheme == "black" || appTheme == "dark") {
        ComposeColor(0xFF121212)
      } else {
        MaterialTheme.colorScheme.surface
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(vertical = 48.dp)
          .clickable(enabled = true, onClick = { finish() }),
        contentAlignment = Alignment.Center,
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = false) {},
          color = dialogBg,
          tonalElevation = 6.dp,
        ) {
          ListWidgetConfigContent(
            appWidgetId = appWidgetId,
            onSave = { ListWidgetConfigActions.onSave(this@handleOnCreate, appWidgetId) },
            onDismiss = { ListWidgetConfigActions.onDismiss(this@handleOnCreate) },
          )
        }
      }
    }
  }
}
