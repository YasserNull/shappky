package com.yassernull.shappky.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R

@Composable
fun formatInterval(ms: Long): String = when {
  ms >= 60000L && ms % 60000L == 0L -> {
    stringResource(R.string.refresh_interval_min, (ms / 60000L).toInt())
  }
  ms >= 1000L && ms % 1000L == 0L -> {
    stringResource(R.string.refresh_interval_sec, (ms / 1000L).toInt())
  }
  else -> {
    stringResource(R.string.refresh_interval_value, ms.toInt())
  }
}
