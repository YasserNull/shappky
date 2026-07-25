package com.yassernull.shappky.ui.dialogs

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.AppDetailedInfo
import com.yassernull.shappky.ui.components.DrawableIcon
import com.yassernull.shappky.ui.components.InfoRow
import com.yassernull.shappky.ui.components.ProcessRow

@Composable
fun AppInfoDialog(
  info: AppDetailedInfo,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 24.dp),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          DrawableIcon(info.app.appIcon)
          Spacer(modifier = Modifier.width(16.dp))
          Column {
            Text(
              text = info.app.appName,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = info.app.packageName,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        InfoRow(label = stringResource(R.string.pid_label), value = info.pid)
        InfoRow(label = stringResource(R.string.user_label), value = info.user)
        InfoRow(label = stringResource(R.string.is_foreground_label), value = if (info.isForeground) stringResource(R.string.yes) else stringResource(R.string.no))
        InfoRow(label = stringResource(R.string.is_persistent_label), value = if (info.app.isPersistentApp) stringResource(R.string.yes) else stringResource(R.string.no))
        InfoRow(label = stringResource(R.string.cpu_usage_label), value = info.cpuUsage)
        InfoRow(label = stringResource(R.string.threads_label), value = info.threads)
        InfoRow(label = stringResource(R.string.total_ram_usage_label), value = Formatter.formatFileSize(context, info.totalRamKb * 1024L))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = stringResource(R.string.all_app_processes),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 8.dp),
        )

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        ) {
          LazyColumn {
            items(info.processes) { process ->
              ProcessRow(process)
              if (process != info.processes.last()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.close))
          }
        }
      }
    }
  }
}
