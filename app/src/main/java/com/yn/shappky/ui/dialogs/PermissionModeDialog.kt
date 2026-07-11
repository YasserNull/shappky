package com.yn.shappky.ui.dialogs

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.topjohnwu.superuser.Shell
import com.yn.shappky.R
import rikka.shizuku.Shizuku

@Composable
fun PermissionModeDialog(
  permissionMode: String,
  onModeSelected: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val options = arrayOf(
    stringResource(R.string.permission_mode_shizuku),
    stringResource(R.string.permission_mode_root),
  )
  val enabled = booleanArrayOf(
    Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED,
    Shell.getShell().isRoot,
  )
  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.permission_mode_dialog_title)) },
    text = {
      Column {
        options.forEachIndexed { index, label ->
          val mode = if (index == 1) "root" else "shizuku"
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(enabled = enabled[index]) { onModeSelected(mode) }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = permissionMode == mode, onClick = null, enabled = enabled[index])
            Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled[index]) 1f else 0.38f))
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}
