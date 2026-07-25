package com.yassernull.shappky.ui.activities.main

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import com.yassernull.shappky.ui.components.CheckableMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainToolbar(
  appsCount: Int,
  hasSelection: Boolean,
  isServiceRunning: Boolean,
  showUserApps: Boolean,
  showSystemApps: Boolean,
  showPersistentApps: Boolean,
  showProtectedApps: Boolean,
  onOpenTriggers: () -> Unit,
  onSelectAll: (Boolean) -> Unit,
  onToggleService: (Boolean) -> Unit,
  onToggleShowUserApps: () -> Unit,
  onToggleShowSystemApps: () -> Unit,
  onToggleShowPersistentApps: () -> Unit,
  onToggleShowProtectedApps: () -> Unit,
  onOpenSortDialog: () -> Unit,
  onOpenFilterDialog: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenDonate: () -> Unit,
) {
  var showMenu by remember { mutableStateOf(false) }

  TopAppBar(
    title = {
      Column {
        Text(stringResource(R.string.app_name))
        Text(
          text = stringResource(R.string.running_apps_count, appsCount),
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
          fontSize = 12.sp,
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    actions = {
      IconButton(onClick = onOpenTriggers) {
        Icon(
          Icons.Filled.Bolt,
          contentDescription = stringResource(R.string.triggers),
        )
      }
      IconButton(onClick = { onSelectAll(!hasSelection) }) {
        Icon(
          if (hasSelection) Icons.Filled.Deselect else Icons.Filled.SelectAll,
          contentDescription = if (hasSelection) {
            stringResource(R.string.unselect_all)
          } else {
            stringResource(R.string.select_all)
          },
        )
      }
      IconButton(onClick = { showMenu = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more))
      }
      DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        CheckableMenuItem(
          text = stringResource(R.string.shappky_service),
          checked = isServiceRunning,
          onClick = {
            showMenu = false
            onToggleService(!isServiceRunning)
          },
        )
        CheckableMenuItem(
          text = stringResource(R.string.show_user_apps),
          checked = showUserApps,
          onClick = {
            showMenu = false
            onToggleShowUserApps()
          },
        )
        CheckableMenuItem(
          text = stringResource(R.string.show_system_apps),
          checked = showSystemApps,
          onClick = {
            showMenu = false
            onToggleShowSystemApps()
          },
        )
        CheckableMenuItem(
          text = stringResource(R.string.show_persistent_apps),
          checked = showPersistentApps,
          onClick = {
            showMenu = false
            onToggleShowPersistentApps()
          },
        )
        CheckableMenuItem(
          text = stringResource(R.string.show_protected_apps),
          checked = showProtectedApps,
          onClick = {
            showMenu = false
            onToggleShowProtectedApps()
          },
        )
        DropdownMenuItem(
          text = { Text(stringResource(R.string.sort_apps)) },
          leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) },
          onClick = {
            showMenu = false
            onOpenSortDialog()
          },
        )
        DropdownMenuItem(
          text = { Text(stringResource(R.string.apps_filter)) },
          leadingIcon = { Icon(Icons.Filled.FilterList, null) },
          onClick = {
            showMenu = false
            onOpenFilterDialog()
          },
        )
        DropdownMenuItem(
          text = { Text(stringResource(R.string.settings)) },
          leadingIcon = { Icon(Icons.Filled.Settings, null) },
          onClick = {
            showMenu = false
            onOpenSettings()
          },
        )
        DropdownMenuItem(
          text = { Text(stringResource(R.string.donate)) },
          leadingIcon = { Icon(Icons.Filled.Favorite, null) },
          onClick = {
            showMenu = false
            onOpenDonate()
          },
        )
      }
    },
  )
}
