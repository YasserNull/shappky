package com.yassernull.shappky.ui.activities.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R
import com.yassernull.shappky.ui.components.ActionSettingRow
import com.yassernull.shappky.ui.components.SettingsHeader

@Composable
fun AboutSection() {
  val context = LocalContext.current

  fun openUrl(url: String) {
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {}
  }

  val donateUrl = stringResource(R.string.donate_url)
  val sourceCodeUrl = stringResource(R.string.source_code_url)

  SettingsHeader(text = stringResource(R.string.settings_about))
  ActionSettingRow(
    icon = Icons.Filled.Favorite,
    title = stringResource(R.string.donate),
    summary = stringResource(R.string.donate_summary),
    onClick = { openUrl(donateUrl) },
  )
  ActionSettingRow(
    icon = Icons.Filled.Code,
    title = stringResource(R.string.source_code),
    summary = stringResource(R.string.source_code_summary),
    onClick = { openUrl(sourceCodeUrl) },
  )
}
