package com.yassernull.shappky.ui.activities.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsContent(
  onBack: () -> Unit,
) {
  Scaffold(
    topBar = { SettingsTopAppBar(onBack = onBack) },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .padding(padding)
        .verticalScroll(rememberScrollState()),
    ) {
      LanguageSection()
      ThemeSection()
      PermissionsSection()
      ServiceSection()
      RamUsageSection()
      AppsListSection()
      AboutSection()
    }
  }
}
