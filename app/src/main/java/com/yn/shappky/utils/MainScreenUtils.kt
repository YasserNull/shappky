package com.yn.shappky.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckableMenuItem(text: String, checked: Boolean, onClick: () -> Unit) {
  DropdownMenuItem(
    text = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
          Checkbox(
            checked = checked,
            onCheckedChange = { onClick() },
            modifier = Modifier.size(24.dp),
          )
        }
        Spacer(Modifier.width(12.dp))
        Text(text)
      }
    },
    contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
    onClick = onClick,
  )
}
