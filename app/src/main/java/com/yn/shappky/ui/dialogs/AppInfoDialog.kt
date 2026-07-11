package com.yn.shappky.ui.dialogs

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yn.shappky.data.models.AppDetailedInfo
import com.yn.shappky.data.models.ProcessInfo
import com.yn.shappky.ui.components.DrawableIcon

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

        InfoRow(label = "PID", value = info.pid)
        InfoRow(label = "User", value = info.user)
        InfoRow(label = "Is Foreground", value = if (info.isForeground) "Yes" else "No")
        InfoRow(label = "Is Persistent", value = if (info.app.isPersistentApp) "Yes" else "No")
        InfoRow(label = "CPU Usage", value = info.cpuUsage)
        InfoRow(label = "Threads", value = info.threads)
        InfoRow(label = "Total RAM Usage", value = Formatter.formatFileSize(context, info.totalRamKb * 1024L))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "All App Processes",
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
            Text("Close")
          }
        }
      }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
private fun ProcessRow(process: ProcessInfo) {
  val context = LocalContext.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
  ) {
    Text(
      text = process.name,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "PID: ${process.pid}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = Formatter.formatFileSize(context, process.ramKb * 1024L),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
