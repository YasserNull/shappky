package com.yn.shappky.ui.activities.serviceCustomization

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceCustomizationToolbar(
  onBack: () -> Unit,
  onSave: () -> Unit,
) {
  TopAppBar(
    title = { Text(stringResource(R.string.customize_service)) },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
      }
    },
    actions = {
      IconButton(onClick = onSave) {
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
