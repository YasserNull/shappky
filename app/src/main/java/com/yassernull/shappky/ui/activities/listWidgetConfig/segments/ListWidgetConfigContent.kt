package com.yassernull.shappky.ui.activities.listWidgetConfig

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import com.yassernull.shappky.providers.ShappkyListWidgetProvider
import com.yassernull.shappky.ui.activities.listWidgetConfig.sections.ListWidgetConfigAppsList
import com.yassernull.shappky.ui.activities.listWidgetConfig.sections.ListWidgetConfigRamUsageBar
import com.yassernull.shappky.ui.activities.listWidgetConfig.sections.ListWidgetConfigTheme

@Composable
fun ListWidgetConfigContent(appWidgetId: Int, onSave: () -> Unit, onDismiss: () -> Unit) {
  val context = androidx.compose.ui.platform.LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(20.dp)
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(R.string.widget_settings),
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(bottom = 16.dp),
    )

    ListWidgetConfigTheme(appWidgetId = appWidgetId)
    ListWidgetConfigRamUsageBar(appWidgetId = appWidgetId)
    ListWidgetConfigAppsList(appWidgetId = appWidgetId)

    Spacer(modifier = Modifier.height(24.dp))

    // ACTION BUTTONS
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
      ) {
        Text(stringResource(R.string.cancel))
      }

      Button(
        onClick = {
          val appWidgetManager = AppWidgetManager.getInstance(context)
          ShappkyListWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)
          @Suppress("DEPRECATION")
          appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
          ShappkyListWidgetProvider.startAutoRefresh(context)
          onSave()
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp),
      ) {
        Text(stringResource(R.string.save))
      }
    }
  }
}
