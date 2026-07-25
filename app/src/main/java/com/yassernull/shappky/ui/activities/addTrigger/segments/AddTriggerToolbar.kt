package com.yassernull.shappky.ui.activities.addTrigger

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTriggerToolbar(
  isEditing: Boolean,
  onSaveClick: () -> Unit,
  onBackClick: () -> Unit,
) {
  TopAppBar(
    title = {
      Text(
        if (isEditing) {
          stringResource(R.string.edit_trigger)
        } else {
          stringResource(R.string.add_trigger)
        },
      )
    },
    navigationIcon = {
      IconButton(onClick = onBackClick) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
      }
    },
    actions = {
      IconButton(onClick = onSaveClick) {
        Icon(Icons.Filled.Save, contentDescription = stringResource(R.string.save))
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface,
      actionIconContentColor = MaterialTheme.colorScheme.onSurface,
    ),
  )
}
