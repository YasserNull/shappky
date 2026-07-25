package com.yassernull.shappky.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R
import com.yassernull.shappky.utils.languageFromIndex

@Composable
fun LanguageDialog(
  languageValue: String,
  options: Array<String>,
  onLanguageSelected: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.language_dialog_title)) },
    text = {
      Column {
        options.forEachIndexed { index, label ->
          val newLanguage = languageFromIndex(index)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onLanguageSelected(newLanguage) }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            RadioButton(selected = languageValue == newLanguage, onClick = null)
            Text(label)
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
