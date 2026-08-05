package com.yassernull.shappky.ui.activities.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.RamState
import com.yassernull.shappky.data.models.AppDetailedInfo
import com.yassernull.shappky.data.models.AppModel
import com.yassernull.shappky.ui.activities.main.logic.AppsList
import com.yassernull.shappky.ui.activities.main.logic.AppsRamUsage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
  apps: List<AppModel>,
  ramState: RamState,
  hasPermission: Boolean,
  isLoadingBackgroundApps: Boolean,
  showUserApps: Boolean,
  showSystemApps: Boolean,
  showPersistentApps: Boolean,
  showProtectedApps: Boolean,
  initialSortMode: String,
  initialSortDescending: Boolean,
  sortByName: String,
  sortByRam: String,
  hiddenApps: Set<String>,
  onSelectAll: (Boolean) -> Unit,
  onRefresh: () -> Unit,
  onToggleShowUserApps: () -> Unit,
  onToggleShowSystemApps: () -> Unit,
  onToggleShowPersistentApps: () -> Unit,
  onToggleShowProtectedApps: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenDonate: () -> Unit,
  onKillSelected: () -> Unit,
  onToggleApp: (AppModel) -> Unit,
  onKillApp: (AppModel, Boolean) -> Unit,
  onApplySort: (sortMode: String, descending: Boolean) -> Unit,
  onLoadAllApps: (((List<AppModel>) -> Unit)) -> Unit,
  onSaveHiddenApps: (Set<String>) -> Unit,
  onFilterSaved: () -> Unit,
  onOpenTriggers: () -> Unit,
  isServiceRunning: Boolean,
  onToggleService: (Boolean) -> Unit,
  onAppLongClick: (AppModel) -> Unit,
  selectedAppForInfo: AppDetailedInfo?,
  onDismissAppInfo: () -> Unit,
) {
  var showSortDialog by remember { mutableStateOf(false) }
  var showFilterDialog by remember { mutableStateOf(false) }
  val hasSelection = apps.any { it.isSelected }

  Scaffold(
    topBar = {
      MainToolbar(
        appsCount = apps.size,
        hasSelection = hasSelection,
        isServiceRunning = isServiceRunning,
        showUserApps = showUserApps,
        showSystemApps = showSystemApps,
        showPersistentApps = showPersistentApps,
        showProtectedApps = showProtectedApps,
        onOpenTriggers = onOpenTriggers,
        onSelectAll = onSelectAll,
        onToggleService = onToggleService,
        onToggleShowUserApps = onToggleShowUserApps,
        onToggleShowSystemApps = onToggleShowSystemApps,
        onToggleShowPersistentApps = onToggleShowPersistentApps,
        onToggleShowProtectedApps = onToggleShowProtectedApps,
        onOpenSortDialog = { showSortDialog = true },
        onOpenFilterDialog = { showFilterDialog = true },
        onOpenSettings = onOpenSettings,
        onOpenDonate = onOpenDonate,
      )
    },
    floatingActionButton = {
      MainFab(
        hasSelection = hasSelection,
        onKillSelected = onKillSelected,
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .padding(padding),
    ) {
      if (!hasPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(
            text = stringResource(R.string.permission_denied),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 16.sp,
          )
        }
        return@Column
      }
      AppsRamUsage(ramState)
      AppsList(
        apps = apps,
        isLoadingBackgroundApps = isLoadingBackgroundApps,
        onRefresh = onRefresh,
        onToggleApp = onToggleApp,
        onKillApp = onKillApp,
        onAppLongClick = onAppLongClick,
      )
    }
  }

  MainDialogs(
    showSortDialog = showSortDialog,
    onDismissSortDialog = { showSortDialog = false },
    initialSortMode = initialSortMode,
    initialSortDescending = initialSortDescending,
    sortByName = sortByName,
    sortByRam = sortByRam,
    onApplySort = { mode, desc ->
      onApplySort(mode, desc)
      showSortDialog = false
    },
    showFilterDialog = showFilterDialog,
    onDismissFilterDialog = { showFilterDialog = false },
    hiddenApps = hiddenApps,
    onLoadAllApps = onLoadAllApps,
    onSaveHiddenApps = onSaveHiddenApps,
    onFilterSaved = {
      showFilterDialog = false
      onFilterSaved()
    },
    selectedAppForInfo = selectedAppForInfo,
    onDismissAppInfo = onDismissAppInfo,
  )
}
