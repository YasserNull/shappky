package com.yassernull.shappky.ui.activities.settings

import android.content.Context
import android.content.Intent
import android.os.Process
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.SettingsBackupManager
import com.yassernull.shappky.ui.components.ActionSettingRow
import com.yassernull.shappky.ui.components.SettingsHeader
import com.yassernull.shappky.ui.dialogs.RestartDialog

@Composable
fun BackupSection() {
  val context = LocalContext.current
  var showRestartDialog by remember { mutableStateOf(false) }

  val exportLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.CreateDocument("text/xml"),
  ) { uri ->
    if (uri != null) {
      val success = SettingsBackupManager.exportSettings(context, uri)
      Toast.makeText(
        context,
        if (success) context.getString(R.string.export_settings_success) else context.getString(R.string.export_settings_failed),
        Toast.LENGTH_SHORT,
      ).show()
    }
  }

  val importLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      val success = SettingsBackupManager.importSettings(context, uri)
      Toast.makeText(
        context,
        if (success) context.getString(R.string.import_settings_success) else context.getString(R.string.import_settings_failed),
        Toast.LENGTH_SHORT,
      ).show()
      if (success) {
        showRestartDialog = true
      }
    }
  }

  SettingsHeader(text = stringResource(R.string.settings_backup))
  ActionSettingRow(
    icon = Icons.Filled.FileDownload,
    title = stringResource(R.string.export_settings),
    summary = stringResource(R.string.export_settings_summary),
    onClick = { exportLauncher.launch(SettingsBackupManager.EXPORT_FILE_NAME) },
  )
  ActionSettingRow(
    icon = Icons.Filled.FileUpload,
    title = stringResource(R.string.import_settings),
    summary = stringResource(R.string.import_settings_summary),
    onClick = { importLauncher.launch(arrayOf("text/xml", "application/xml", "application/octet-stream", "*/*")) },
  )

  if (showRestartDialog) {
    RestartDialog(
      onRestart = {
        restartApp(context)
      },
      onDismiss = { showRestartDialog = false },
    )
  }
}

private fun restartApp(context: Context) {
  val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
  intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
  context.startActivity(intent)
  Process.killProcess(Process.myPid())
}
