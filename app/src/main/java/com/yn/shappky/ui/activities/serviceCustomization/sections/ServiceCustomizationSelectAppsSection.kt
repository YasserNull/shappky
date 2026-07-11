package com.yn.shappky.ui.activities.serviceCustomization.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yn.shappky.R
import com.yn.shappky.ui.activities.serviceCustomization.ServiceCustomizationSelectAppsDialogs

@Composable
fun ServiceCustomizationSelectAppsSection(
  selectUserApps: Boolean,
  onSelectUserAppsChange: (Boolean) -> Unit,
  selectSystemApps: Boolean,
  onSelectSystemAppsChange: (Boolean) -> Unit,
  excludedApps: Set<String>,
  onExcludedAppsChange: (Set<String>) -> Unit,
  manuallySelectedApps: Set<String>,
  onManuallySelectedAppsChange: (Set<String>) -> Unit,
) {
  val context = LocalContext.current
  var showExcludeDialog by remember { mutableStateOf(false) }
  var showManualDialog by remember { mutableStateOf(false) }

  ServiceCustomizationSelectAppsDialogs(
    context = context,
    showExcludeDialog = showExcludeDialog,
    onDismissExcludeDialog = { showExcludeDialog = false },
    onExcludedAppsSaved = {
      onExcludedAppsChange(it)
      showExcludeDialog = false
    },
    excludedApps = excludedApps,
    showManualDialog = showManualDialog,
    onDismissManualDialog = { showManualDialog = false },
    onManuallySelectedAppsSaved = {
      onManuallySelectedAppsChange(it)
      showManualDialog = false
    },
    manuallySelectedApps = manuallySelectedApps,
  )

  Text(
    text = stringResource(R.string.select_apps),
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(vertical = 8.dp),
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onSelectUserAppsChange(!selectUserApps) }
      .padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.user_apps),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
    Checkbox(
      checked = selectUserApps,
      onCheckedChange = onSelectUserAppsChange,
    )
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onSelectSystemAppsChange(!selectSystemApps) }
      .padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.system_apps),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
    Checkbox(
      checked = selectSystemApps,
      onCheckedChange = onSelectSystemAppsChange,
    )
  }

  HorizontalDivider(
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    modifier = Modifier.padding(vertical = 8.dp),
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { showExcludeDialog = true }
      .padding(vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Filled.Block,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.exclude_apps),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "${excludedApps.size}",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
      )
    }
  }

  HorizontalDivider(
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    modifier = Modifier.padding(vertical = 8.dp),
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { showManualDialog = true }
      .padding(vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.manually_select_apps),
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "${manuallySelectedApps.size}",
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
