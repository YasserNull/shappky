package com.yn.shappky.ui.activities.main

import androidx.compose.runtime.Composable
import com.yn.shappky.data.models.AppDetailedInfo
import com.yn.shappky.data.models.AppModel
import com.yn.shappky.ui.dialogs.AppInfoDialog
import com.yn.shappky.ui.dialogs.FilterDialog
import com.yn.shappky.ui.dialogs.SortDialog

@Composable
fun MainDialogs(
  showSortDialog: Boolean,
  onDismissSortDialog: () -> Unit,
  initialSortMode: String,
  initialSortDescending: Boolean,
  sortByName: String,
  sortByRam: String,
  onApplySort: (String, Boolean) -> Unit,
  showFilterDialog: Boolean,
  onDismissFilterDialog: () -> Unit,
  hiddenApps: Set<String>,
  onLoadAllApps: (((List<AppModel>) -> Unit)) -> Unit,
  onSaveHiddenApps: (Set<String>) -> Unit,
  onFilterSaved: () -> Unit,
  selectedAppForInfo: AppDetailedInfo?,
  onDismissAppInfo: () -> Unit,
) {
  if (showSortDialog) {
    SortDialog(
      initialSortMode = initialSortMode,
      initialDescending = initialSortDescending,
      sortByName = sortByName,
      sortByRam = sortByRam,
      onDismiss = onDismissSortDialog,
      onApply = onApplySort,
    )
  }

  if (showFilterDialog) {
    FilterDialog(
      hiddenApps = hiddenApps,
      loadAllApps = onLoadAllApps,
      onSaveHiddenApps = onSaveHiddenApps,
      onDismiss = onDismissFilterDialog,
      onSaved = onFilterSaved,
    )
  }

  selectedAppForInfo?.let { info ->
    AppInfoDialog(
      info = info,
      onDismiss = onDismissAppInfo,
    )
  }
}
