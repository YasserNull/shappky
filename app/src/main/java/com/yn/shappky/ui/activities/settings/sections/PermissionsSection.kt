package com.yn.shappky.ui.activities.settings

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R
import com.yn.shappky.ui.components.SettingsHeader
import com.yn.shappky.ui.components.ValueSettingRow
import com.yn.shappky.ui.dialogs.PermissionModeDialog

@Composable
fun PermissionsSection() {
  val context = LocalContext.current
  val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

  var permissionMode by remember { mutableStateOf(sharedPreferences.getString("permissionMode", "shizuku") ?: "shizuku") }
  var showPermissionDialog by remember { mutableStateOf(false) }

  SettingsHeader(text = stringResource(R.string.settings_permissions))
  ValueSettingRow(
    icon = Icons.Filled.Security,
    title = stringResource(R.string.permission_mode),
    summary = stringResource(R.string.permission_mode_summary),
    value = if (permissionMode == "root") {
      stringResource(R.string.permission_mode_root)
    } else {
      stringResource(R.string.permission_mode_shizuku)
    },
    onClick = { showPermissionDialog = true },
  )

  if (showPermissionDialog) {
    PermissionModeDialog(
      permissionMode = permissionMode,
      onModeSelected = { newMode ->
        permissionMode = newMode
        sharedPreferences.edit().putString("permissionMode", newMode).apply()
        showPermissionDialog = false
      },
      onDismiss = { showPermissionDialog = false },
    )
  }
}
