package com.yn.shappky.ui.activities.widgetConfig

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yn.shappky.R
import com.yn.shappky.core.preferences.PREFERENCES_NAME
import com.yn.shappky.core.preferences.WidgetPreferences
import com.yn.shappky.providers.ShappkyWidgetProvider
import com.yn.shappky.utils.TriggerManager
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun WidgetConfigScreen(appWidgetId: Int, onSave: () -> Unit, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) }
  val triggers = remember { TriggerManager.getTriggers(context) }

  var selectedTriggerId by remember {
    val saved = prefs.getString(WidgetPreferences.getTriggerIdKey(appWidgetId), "") ?: ""
    mutableStateOf(saved.ifEmpty { triggers.firstOrNull()?.id ?: "" })
  }

  var selectedBgSize by remember {
    mutableStateOf(prefs.getInt(WidgetPreferences.getBgSizeKey(appWidgetId), 48))
  }

  var selectedIconColor by remember {
    mutableStateOf(prefs.getInt(WidgetPreferences.getIconColorKey(appWidgetId), Color.WHITE))
  }

  val defaultBlue = remember { context.getColor(R.color.control_blue) }
  var selectedBgColor by remember {
    mutableStateOf(prefs.getInt(WidgetPreferences.getBgColorKey(appWidgetId), defaultBlue))
  }

  val appTheme = remember { prefs.getString("appTheme", "dark") ?: "dark" }
  val dialogBg = if (appTheme == "black" || appTheme == "dark") {
    ComposeColor(0xFF121212)
  } else {
    MaterialTheme.colorScheme.surface
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(vertical = 48.dp)
      .clickable(enabled = true, onClick = onDismiss),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .wrapContentHeight()
        .clickable(enabled = false) {},
      color = dialogBg,
      shape = MaterialTheme.shapes.large,
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(R.string.widget_settings),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))

        WidgetConfigTheme(
          appWidgetId = appWidgetId,
          selectedBgSize = selectedBgSize,
          onBgSizeChange = { selectedBgSize = it },
          selectedIconColor = selectedIconColor,
          onIconColorChange = { selectedIconColor = it },
          selectedBgColor = selectedBgColor,
          onBgColorChange = { selectedBgColor = it },
        )

        Spacer(Modifier.height(16.dp))

        WidgetConfigTriggers(
          triggers = triggers,
          selectedTriggerId = selectedTriggerId,
          onTriggerSelected = { selectedTriggerId = it },
        )

        Spacer(Modifier.height(24.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.cancel))
          }
          Spacer(Modifier.width(8.dp))
          TextButton(onClick = {
            selectedBgColor = defaultBlue
            selectedIconColor = Color.WHITE
            selectedBgSize = 48
          }) {
            Text(stringResource(R.string.reset))
          }
          Spacer(Modifier.width(8.dp))
          Button(
            onClick = {
              val iconSize = 50

              prefs.edit().apply {
                putInt(WidgetPreferences.getIconSizeKey(appWidgetId), iconSize)
                putInt(WidgetPreferences.getIconColorKey(appWidgetId), selectedIconColor)
                putInt(WidgetPreferences.getBgColorKey(appWidgetId), selectedBgColor)
                putString(WidgetPreferences.getTriggerIdKey(appWidgetId), selectedTriggerId)
                putBoolean(WidgetPreferences.getShowLabelKey(appWidgetId), false)
                putInt(WidgetPreferences.getBgSizeKey(appWidgetId), selectedBgSize)
                apply()
              }

              val appWidgetManager = AppWidgetManager.getInstance(context)
              ShappkyWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
              onSave()
            },
            enabled = selectedTriggerId.isNotEmpty(),
          ) {
            Text(stringResource(R.string.save))
          }
        }
      }
    }
  }
}
