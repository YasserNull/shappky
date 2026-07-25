package com.yassernull.shappky.ui.activities.settings

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.yassernull.shappky.R
import com.yassernull.shappky.ui.activities.serviceCustomization.ServiceCustomizationActivity
import com.yassernull.shappky.ui.activities.triggers.TriggersActivity
import com.yassernull.shappky.ui.components.ActionSettingRow
import com.yassernull.shappky.ui.components.SettingsHeader

@Composable
fun ServiceSection() {
  val context = LocalContext.current

  SettingsHeader(text = stringResource(R.string.settings_service))
  ActionSettingRow(
    painter = painterResource(R.drawable.ic_shappky),
    title = stringResource(R.string.customize_service),
    summary = stringResource(R.string.customize_service_summary),
    onClick = {
      context.startActivity(Intent(context, ServiceCustomizationActivity::class.java))
    },
  )

  SettingsHeader(text = stringResource(R.string.triggers))
  ActionSettingRow(
    icon = Icons.Filled.Bolt,
    title = stringResource(R.string.triggers),
    summary = stringResource(R.string.triggers_summary),
    onClick = {
      context.startActivity(Intent(context, TriggersActivity::class.java))
    },
  )
}
