package com.yassernull.shappky.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.utils.buildTriggerSummary

@Composable
fun TriggerItem(
  trigger: TriggerModel,
  onExecute: () -> Unit,
  onDelete: () -> Unit,
  onToggleState: (Boolean) -> Unit,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .graphicsLayer(alpha = if (trigger.isEnabled) 1f else 0.5f),
      ) {
        Text(
          text = trigger.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = trigger.buildTriggerSummary(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      Switch(
        checked = trigger.isEnabled,
        onCheckedChange = { isChecked ->
          onToggleState(isChecked)
        },
        modifier = Modifier.padding(horizontal = 8.dp),
      )

      IconButton(onClick = onExecute, enabled = trigger.isEnabled) {
        Icon(
          Icons.Filled.PlayArrow,
          contentDescription = "Run",
          tint = if (trigger.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
          modifier = Modifier.size(28.dp),
        )
      }

      IconButton(onClick = onDelete) {
        Icon(
          Icons.Filled.Delete,
          contentDescription = stringResource(R.string.delete_trigger),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}
