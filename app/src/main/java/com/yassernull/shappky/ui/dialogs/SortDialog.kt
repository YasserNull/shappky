package com.yassernull.shappky.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R
import com.yassernull.shappky.ui.components.DialogRadioRow

@Composable
fun SortDialog(
  initialSortMode: String,
  initialDescending: Boolean,
  sortByName: String,
  sortByRam: String,
  sortByType: String = "type",
  onDismiss: () -> Unit,
  onApply: (sortMode: String, descending: Boolean) -> Unit,
) {
  var selectedSortMode by mutableStateOf(initialSortMode)
  var descending by mutableStateOf(initialDescending)

  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.sort_apps_title)) },
    text = {
      Column {
        DialogRadioRow(
          text = stringResource(R.string.sort_by_name),
          selected = selectedSortMode == sortByName,
          onClick = { selectedSortMode = sortByName },
        )
        DialogRadioRow(
          text = stringResource(R.string.sort_by_ram_usage),
          selected = selectedSortMode == sortByRam,
          onClick = { selectedSortMode = sortByRam },
        )
        DialogRadioRow(
          text = stringResource(R.string.sort_by_type),
          selected = selectedSortMode == sortByType,
          onClick = { selectedSortMode = sortByType },
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { descending = !descending }
            .padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(checked = descending, onCheckedChange = { descending = it })
          Text(stringResource(R.string.sort_descending))
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onApply(selectedSortMode, descending) }) {
        Text(stringResource(R.string.ok))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
    },
  )
}
