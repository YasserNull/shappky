package com.yn.shappky.ui.activities.triggers

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yn.shappky.R
import com.yn.shappky.data.models.TriggerModel
import com.yn.shappky.ui.activities.addTrigger.AddTriggerActivity
import com.yn.shappky.ui.components.TriggerItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggersContent(
  triggers: List<TriggerModel>,
  onBack: () -> Unit,
  onExecute: (TriggerModel) -> Unit,
  onDelete: (TriggerModel) -> Unit,
  onToggleState: (TriggerModel, Boolean) -> Unit,
) {
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.triggers)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
          actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          context.startActivity(Intent(context, AddTriggerActivity::class.java))
        },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_trigger))
      }
    },
  ) { padding ->
    if (triggers.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.triggers_empty),
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          fontSize = 16.sp,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .padding(horizontal = 16.dp),
      ) {
        items(triggers, key = { it.id }) { trigger ->
          TriggerItem(
            trigger = trigger,
            onExecute = { onExecute(trigger) },
            onDelete = { onDelete(trigger) },
            onToggleState = { isChecked -> onToggleState(trigger, isChecked) },
            onClick = {
              val intent = Intent(context, AddTriggerActivity::class.java).apply {
                putExtra(AddTriggerActivity.EXTRA_TRIGGER_ID, trigger.id)
              }
              context.startActivity(intent)
            },
          )
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
  }
}
