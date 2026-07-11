package com.yn.shappky.ui.dialogs

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yn.shappky.R
import com.yn.shappky.data.models.AppModel

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

@Composable
private fun SearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  showUser: Boolean,
  onShowUserChange: (Boolean) -> Unit,
  showSystem: Boolean,
  onShowSystemChange: (Boolean) -> Unit,
  showPersistent: Boolean,
  onShowPersistentChange: (Boolean) -> Unit,
  showProtected: Boolean,
  onShowProtectedChange: (Boolean) -> Unit,
) {
  var showMenu by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
      .padding(horizontal = 12.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(Icons.Filled.Search, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    BasicTextField(
      value = query,
      onValueChange = onQueryChange,
      singleLine = true,
      textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
      modifier = Modifier.weight(1f),
      decorationBox = { inner ->
        if (query.isEmpty()) {
          Text(
            stringResource(R.string.search_apps),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
          )
        }
        inner()
      },
    )
    Box {
      IconButton(onClick = { showMenu = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = null)
      }
      DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
      ) {
        DropdownCheckboxItem(
          text = stringResource(R.string.user_apps),
          checked = showUser,
          onCheckedChange = onShowUserChange,
        )
        DropdownCheckboxItem(
          text = stringResource(R.string.system_apps),
          checked = showSystem,
          onCheckedChange = onShowSystemChange,
        )
        DropdownCheckboxItem(
          text = stringResource(R.string.persistent_apps),
          checked = showPersistent,
          onCheckedChange = onShowPersistentChange,
        )
        DropdownCheckboxItem(
          text = stringResource(R.string.protected_apps),
          checked = showProtected,
          onCheckedChange = onShowProtectedChange,
        )
      }
    }
  }
}

@Composable
private fun DropdownCheckboxItem(
  text: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  DropdownMenuItem(
    text = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Text(text)
      }
    },
    onClick = { onCheckedChange(!checked) },
  )
}

@Composable
private fun DrawableIcon(drawable: Drawable) {
  AndroidView(
    factory = { context ->
      ImageView(context).apply {
        layoutParams = ViewGroup.LayoutParams(48, 48)
        scaleType = ImageView.ScaleType.FIT_CENTER
      }
    },
    update = { imageView -> imageView.setImageDrawable(drawable) },
    modifier = Modifier.size(48.dp),
  )
}
