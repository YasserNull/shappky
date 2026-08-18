package com.yassernull.shappky.ui.activities.serviceCustomization.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
fun ServiceCustomizationKillWhenReachRam(
  killAllOnRamLimit: Boolean,
  onKillAllOnRamLimitChange: (Boolean) -> Unit,
  killAllRamThreshold: Int,
  onKillAllRamThresholdChange: (Int) -> Unit,
  killAppOnRamLimit: Boolean,
  onKillAppOnRamLimitChange: (Boolean) -> Unit,
  killAppRamThreshold: Int,
  onKillAppRamThresholdChange: (Int) -> Unit,
) {
  var showKillAllRamDialog by remember { mutableStateOf(false) }
  var showKillAppRamDialog by remember { mutableStateOf(false) }

  ServiceCustomizationConfigDialogs(
    showDurationDialog = false,
    onDismissDurationDialog = {},
    onServiceDurationSelected = {},
    serviceDuration = 0,
    showKillAllRamDialog = showKillAllRamDialog,
    onDismissKillAllRamDialog = { showKillAllRamDialog = false },
    onKillAllRamConfirmed = { limit ->
      onKillAllRamThresholdChange(limit)
      onKillAllOnRamLimitChange(true)
      showKillAllRamDialog = false
    },
    killAllRamThreshold = killAllRamThreshold,
    showKillAppRamDialog = showKillAppRamDialog,
    onDismissKillAppRamDialog = { showKillAppRamDialog = false },
    onKillAppRamConfirmed = { limit ->
      onKillAppRamThresholdChange(limit)
      onKillAppOnRamLimitChange(true)
      showKillAppRamDialog = false
    },
    killAppRamThreshold = killAppRamThreshold,
  )

  Text(
    text = stringResource(R.string.kill_when_reach_ram_limit),
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(vertical = 8.dp),
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        if (killAllOnRamLimit) {
          onKillAllOnRamLimitChange(false)
        } else {
          showKillAllRamDialog = true
        }
      }
      .padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.service_kill_all_on_ram_limit),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (killAllOnRamLimit && killAllRamThreshold > 0) {
        Text(
          text = stringResource(R.string.ram_limit_enabled_summary, String.format(java.util.Locale.US, "%d", killAllRamThreshold)),
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
      }
    }
    Checkbox(
      checked = killAllOnRamLimit,
      onCheckedChange = { isChecked ->
        if (isChecked) {
          showKillAllRamDialog = true
        } else {
          onKillAllOnRamLimitChange(false)
        }
      },
    )
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        if (killAppOnRamLimit) {
          onKillAppOnRamLimitChange(false)
        } else {
          showKillAppRamDialog = true
        }
      }
      .padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.service_kill_app_on_ram_limit),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (killAppOnRamLimit && killAppRamThreshold > 0) {
        Text(
          text = stringResource(R.string.ram_limit_enabled_summary, String.format(java.util.Locale.US, "%d", killAppRamThreshold)),
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
      }
    }
    Checkbox(
      checked = killAppOnRamLimit,
      onCheckedChange = { isChecked ->
        if (isChecked) {
          showKillAppRamDialog = true
        } else {
          onKillAppOnRamLimitChange(false)
        }
      },
    )
  }

  HorizontalDivider(
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    modifier = Modifier.padding(vertical = 8.dp),
  )
}
