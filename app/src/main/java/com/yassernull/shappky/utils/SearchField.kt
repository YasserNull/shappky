package com.yassernull.shappky.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import com.yassernull.shappky.ui.components.DropdownCheckboxItem

@Composable
fun SearchField(
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
