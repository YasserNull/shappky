package com.yassernull.shappky.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R

@Composable
fun ProtectedAppsSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  showUserApps: Boolean,
  onShowUserAppsChange: (Boolean) -> Unit,
  showSystemApps: Boolean,
  onShowSystemAppsChange: (Boolean) -> Unit,
  showPersistentApps: Boolean,
  onShowPersistentAppsChange: (Boolean) -> Unit,
  isMenuExpanded: Boolean,
  onToggleMenu: () -> Unit,
  onDismissMenu: () -> Unit,
) {
  val focusManager = LocalFocusManager.current

  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = Modifier.fillMaxWidth(),
    placeholder = { Text(stringResource(R.string.search_apps)) },
    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
    trailingIcon = {
      IconButton(onClick = onToggleMenu) {
        Icon(Icons.Default.MoreVert, contentDescription = "Filter")
      }
      DropdownMenu(
        expanded = isMenuExpanded,
        onDismissRequest = onDismissMenu,
      ) {
        DropdownMenuItem(
          text = { Text("User Apps") },
          trailingIcon = { Checkbox(checked = showUserApps, onCheckedChange = onShowUserAppsChange) },
          onClick = { onShowUserAppsChange(!showUserApps) },
        )
        DropdownMenuItem(
          text = { Text("System Apps") },
          trailingIcon = { Checkbox(checked = showSystemApps, onCheckedChange = onShowSystemAppsChange) },
          onClick = { onShowSystemAppsChange(!showSystemApps) },
        )
        DropdownMenuItem(
          text = { Text("Persistent Apps") },
          trailingIcon = { Checkbox(checked = showPersistentApps, onCheckedChange = onShowPersistentAppsChange) },
          onClick = { onShowPersistentAppsChange(!showPersistentApps) },
        )
      }
    },
    singleLine = true,
    shape = RoundedCornerShape(12.dp),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
  )
}
