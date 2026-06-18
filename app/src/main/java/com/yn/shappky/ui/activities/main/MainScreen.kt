package com.yn.shappky.ui.activities.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yn.shappky.R
import com.yn.shappky.model.AppModel
import com.yn.shappky.ui.components.AppRow
import com.yn.shappky.ui.components.RamUsageBar
import com.yn.shappky.ui.dialogs.FilterDialog
import com.yn.shappky.ui.dialogs.SortDialog
import com.yn.shappky.util.RamState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    apps: List<AppModel>,
    ramState: RamState,
    hasPermission: Boolean,
    isLoadingBackgroundApps: Boolean,
    showSystemApps: Boolean,
    showPersistentApps: Boolean,
    initialSortMode: String,
    initialSortDescending: Boolean,
    sortByName: String,
    sortByRam: String,
    hiddenApps: Set<String>,
    onSelectAll: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onToggleShowSystemApps: () -> Unit,
    onToggleShowPersistentApps: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDonate: () -> Unit,
    onKillSelected: () -> Unit,
    onToggleApp: (AppModel) -> Unit,
    onKillApp: (AppModel) -> Unit,
    onApplySort: (sortMode: String, descending: Boolean) -> Unit,
    onLoadAllApps: (((List<AppModel>) -> Unit)) -> Unit,
    onSaveHiddenApps: (Set<String>) -> Unit,
    onFilterSaved: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    val hasSelection = apps.any { it.isSelected }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        Text(
                            text = stringResource(R.string.running_apps_count, apps.size),
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
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_apps)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) },
                            onClick = {
                                showMenu = false
                                showSortDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.apps_filter)) },
                            leadingIcon = { Icon(Icons.Filled.FilterList, null) },
                            onClick = {
                                showMenu = false
                                showFilterDialog = true
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
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = hasSelection,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = onKillSelected,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Cancel, contentDescription = stringResource(R.string.force_stop_selected))
                }
            }
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
            RamUsageBar(ramState)
            PullToRefreshBox(
                isRefreshing = isLoadingBackgroundApps,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(apps, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            onToggle = { onToggleApp(app) },
                            onKill = { onKillApp(app) },
                        )
                    }
                }
            }
        }
    }

    if (showSortDialog) {
        SortDialog(
            initialSortMode = initialSortMode,
            initialDescending = initialSortDescending,
            sortByName = sortByName,
            sortByRam = sortByRam,
            onDismiss = { showSortDialog = false },
            onApply = { sortMode, descending ->
                onApplySort(sortMode, descending)
                showSortDialog = false
            },
        )
    }
    if (showFilterDialog) {
        FilterDialog(
            hiddenApps = hiddenApps,
            loadAllApps = onLoadAllApps,
            onSaveHiddenApps = onSaveHiddenApps,
            onDismiss = { showFilterDialog = false },
            onSaved = {
                showFilterDialog = false
                onFilterSaved()
            },
        )
    }
}

@Composable
private fun CheckableMenuItem(text: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(text)
            }
        },
        contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
        onClick = onClick,
    )
}
