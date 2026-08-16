package com.yassernull.shappky.ui.activities.addTrigger.sections

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
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerRule
import com.yassernull.shappky.ui.activities.serviceCustomization.sections.getDurationLabel
import com.yassernull.shappky.ui.dialogs.ServiceDurationDialog

const val DEFAULT_TRIGGER_SERVICE_DURATION_MS = 30000L

fun defaultDurationForRule(ruleType: RuleType): Long = when (ruleType) {
  RuleType.APP_OPENED,
  RuleType.APP_RESUMED,
  RuleType.APP_PAUSED,
  RuleType.APP_EXITED,
  RuleType.APP_KILLED,
  RuleType.APP_BACKGROUND_STARTED,
  RuleType.PHONE_SLEEP,
  RuleType.PHONE_WAKE,
  -> 15000L

  RuleType.KILL_OLDEST_APP,
  RuleType.APP_RAM_EXCEEDED,
  RuleType.RAM_LIMIT_REACHED,
  RuleType.SERVICE_STATE_CHANGED,
  -> 30000L

  RuleType.SPECIFIC_TIME,
  RuleType.APP_INACTIVITY,
  -> 60000L
}

fun defaultDurationForRules(rules: List<TriggerRule>): Long {
  if (rules.isEmpty()) return DEFAULT_TRIGGER_SERVICE_DURATION_MS
  return rules.minOf { defaultDurationForRule(it.type) }
}

@Composable
fun ExecutionSection(
  serviceDuration: Long,
  onServiceDurationChange: (Long) -> Unit,
) {
  var showDurationDialog by remember { mutableStateOf(false) }

  if (showDurationDialog) {
    ServiceDurationDialog(
      currentDurationMs = serviceDuration,
      onDurationSelected = {
        onServiceDurationChange(it)
        showDurationDialog = false
      },
      onDismiss = { showDurationDialog = false },
    )
  }

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
