package com.yassernull.shappky.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.AppModel
import com.yassernull.shappky.ui.components.DrawableIcon
import com.yassernull.shappky.utils.SearchField

@Composable
fun AppSelectionDialog(
  title: String,
  initialSelectedPackages: Set<String>,
  loadAllApps: ((List<AppModel>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
  onSave: (Set<String>) -> Unit,
) {
  var query by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(true) }
  val allApps = remember { mutableStateListOf<AppModel>() }
  var selectedPackages by remember { mutableStateOf(initialSelectedPackages) }

  var showUserAppsFilter by remember { mutableStateOf(true) }
  var showSystemAppsFilter by remember { mutableStateOf(true) }
  var showPersistentAppsFilter by remember { mutableStateOf(true) }
  var showProtectedAppsFilter by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    loadAllApps { result ->
      allApps.clear()
      allApps.addAll(result)
      isLoading = false
    }
  }

  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(modifier = Modifier.height(460.dp)) {
        SearchField(
          query = query,
          onQueryChange = { query = it },
          showUser = showUserAppsFilter,
          onShowUserChange = { showUserAppsFilter = it },
          showSystem = showSystemAppsFilter,
          onShowSystemChange = { showSystemAppsFilter = it },
          showPersistent = showPersistentAppsFilter,
          onShowPersistentChange = { showPersistentAppsFilter = it },
          showProtected = showProtectedAppsFilter,
          onShowProtectedChange = { showProtectedAppsFilter = it },
        )
        Spacer(Modifier.height(8.dp))
        if (isLoading) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else {
          val filtered = allApps.filter { app ->
            val matchesQuery = app.appName.contains(query, ignoreCase = true)
            val matchesType = when {
              app.isProtected -> showProtectedAppsFilter
              app.isPersistentApp -> showPersistentAppsFilter
              app.isSystemApp -> showSystemAppsFilter
              else -> showUserAppsFilter
            }
            matchesQuery && matchesType
          }
          LazyColumn {
            items(filtered, key = { it.packageName }) { app ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    selectedPackages =
                      if (selectedPackages.contains(app.packageName)) {
                        selectedPackages - app.packageName
                      } else {
                        selectedPackages + app.packageName
                      }
                  }
                  .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                DrawableIcon(app.appIcon)
                Spacer(Modifier.width(12.dp))
                Text(
                  text = app.appName,
                  modifier = Modifier.weight(1f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Checkbox(
                  checked = selectedPackages.contains(app.packageName),
                  onCheckedChange = { checked ->
                    selectedPackages =
                      if (checked) {
                        selectedPackages + app.packageName
                      } else {
                        selectedPackages - app.packageName
                      }
                  },
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onSave(selectedPackages)
        },
      ) {
        Text(stringResource(R.string.save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}
