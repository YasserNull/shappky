package com.yassernull.shappky.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.data.models.AppModel

@Composable
fun ProtectedAppsAppList(
  apps: List<AppModel>,
  selectedPackages: Set<String>,
  onToggle: (String) -> Unit,
) {
  apps.forEach { app ->
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onToggle(app.packageName) }
        .padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      DrawableIcon(app.appIcon)
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = app.appName,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodyLarge,
        )
        Text(
          text = app.packageName,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Checkbox(
        checked = selectedPackages.contains(app.packageName),
        onCheckedChange = { onToggle(app.packageName) },
      )
    }
  }
}
