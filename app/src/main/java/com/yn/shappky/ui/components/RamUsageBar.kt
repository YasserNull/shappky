package com.yn.shappky.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yn.shappky.R
import com.yn.shappky.utils.RamState

@Composable
fun RamUsageBar(ramState: RamState) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = if (ramState.totalKb > 0) {
        stringResource(R.string.ram_usage, ramState.usedKb / 1024, ramState.totalKb / 1024)
      } else {
        stringResource(R.string.ram_usage_unknown)
      },
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.width(8.dp))
    LinearProgressIndicator(
      progress = { ramState.progress },
      modifier = Modifier.weight(1f),
    )
  }
}
