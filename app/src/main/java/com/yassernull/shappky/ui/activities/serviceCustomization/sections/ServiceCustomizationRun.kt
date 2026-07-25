package com.yassernull.shappky.ui.activities.serviceCustomization.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import com.yassernull.shappky.ui.activities.serviceCustomization.ServiceCustomizationConfigDialogs

@Composable
fun getDurationLabel(durationMs: Long): String = when (durationMs) {
  15000L -> stringResource(R.string.duration_15s)
  30000L -> stringResource(R.string.duration_30s)
  60000L -> stringResource(R.string.duration_1m)
  120000L -> stringResource(R.string.duration_2m)
  300000L -> stringResource(R.string.duration_5m)
  600000L -> stringResource(R.string.duration_10m)
  else -> "${durationMs / 1000}s"
}

@Composable
fun ServiceCustomizationRun(
  serviceDuration: Long,
  onServiceDurationChange: (Long) -> Unit,
) {
  var showDurationDialog by remember { mutableStateOf(false) }

  ServiceCustomizationConfigDialogs(
    showDurationDialog = showDurationDialog,
    onDismissDurationDialog = { showDurationDialog = false },
    onServiceDurationSelected = {
      onServiceDurationChange(it)
      showDurationDialog = false
    },
    serviceDuration = serviceDuration,
    showKillAllRamDialog = false,
    onDismissKillAllRamDialog = {},
    onKillAllRamConfirmed = {},
    killAllRamThreshold = 0,
    showKillAppRamDialog = false,
    onDismissKillAppRamDialog = {},
    onKillAppRamConfirmed = {},
    killAppRamThreshold = 0,
  )

  Text(
    text = stringResource(R.string.service_execution),
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(vertical = 8.dp),
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { showDurationDialog = true }
      .padding(vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Filled.Schedule,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.service_duration),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = getDurationLabel(serviceDuration),
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
      )
    }
  }

  HorizontalDivider(
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    modifier = Modifier.padding(vertical = 8.dp),
  )
}
