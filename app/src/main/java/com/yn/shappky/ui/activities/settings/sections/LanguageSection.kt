package com.yn.shappky.ui.activities.settings

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.yn.shappky.R
import com.yn.shappky.ui.components.SettingsHeader
import com.yn.shappky.ui.components.ValueSettingRow
import com.yn.shappky.ui.dialogs.LanguageDialog
import com.yn.shappky.utils.LanguageHelper
import com.yn.shappky.utils.getLanguageLabel
import com.yn.shappky.utils.restartApp

@Composable
fun LanguageSection() {
  val context = LocalContext.current
  val sharedPreferences = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
  var languageValue by remember { mutableStateOf(sharedPreferences.getString("appLanguage", "system") ?: "system") }
  var showLanguageDialog by remember { mutableStateOf(false) }

  SettingsHeader(text = stringResource(R.string.language))
  val languageOptions = stringArrayResource(R.array.language_options)

  ValueSettingRow(
    icon = Icons.Filled.Translate,
    title = stringResource(R.string.language),
    summary = stringResource(R.string.language_summary),
    value = getLanguageLabel(languageValue, languageOptions),
    onClick = { showLanguageDialog = true },
  )

  if (showLanguageDialog) {
    LanguageDialog(
      languageValue = languageValue,
      options = languageOptions,
      onLanguageSelected = { newLanguage ->
        if (newLanguage != languageValue) {
          languageValue = newLanguage
          sharedPreferences.edit().putString("appLanguage", newLanguage).commit()
          LanguageHelper.updateLauncherComponent(context, newLanguage)
          LanguageHelper.updateAllWidgets(context)
          context.restartApp()
        }
        showLanguageDialog = false
      },
      onDismiss = { showLanguageDialog = false },
    )
  }
}
