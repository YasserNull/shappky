package com.yn.shappky.ui.activities.serviceCustomization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yn.shappky.data.models.TriggerRule

data class ServiceSettings(
  val selectUserApps: Boolean,
  val selectSystemApps: Boolean,
  val excludedApps: Set<String>,
  val manuallySelectedApps: Set<String>,
  val serviceDuration: Long,
  val killAllOnRamLimit: Boolean,
  val killAllRamThreshold: Int,
  val killAppOnRamLimit: Boolean,
  val killAppRamThreshold: Int,
  val enableRules: List<TriggerRule>,
  val disableRules: List<TriggerRule>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceCustomizationContent(
  initialSettings: ServiceSettings,
  onSave: (ServiceSettings) -> Unit,
  onBack: () -> Unit,
) {
  var selectUserApps by remember { mutableStateOf(initialSettings.selectUserApps) }
  var selectSystemApps by remember { mutableStateOf(initialSettings.selectSystemApps) }
  var excludedApps by remember { mutableStateOf(initialSettings.excludedApps) }
  var manuallySelectedApps by remember { mutableStateOf(initialSettings.manuallySelectedApps) }
  var serviceDuration by remember { mutableStateOf(initialSettings.serviceDuration) }
  var killAllOnRamLimit by remember { mutableStateOf(initialSettings.killAllOnRamLimit) }
  var killAllRamThreshold by remember { mutableStateOf(initialSettings.killAllRamThreshold) }
  var killAppOnRamLimit by remember { mutableStateOf(initialSettings.killAppOnRamLimit) }
  var killAppRamThreshold by remember { mutableStateOf(initialSettings.killAppRamThreshold) }
  var enableRules by remember { mutableStateOf(initialSettings.enableRules) }
  var disableRules by remember { mutableStateOf(initialSettings.disableRules) }

  Scaffold(
    topBar = {
      ServiceCustomizationToolbar(
        onBack = onBack,
        onSave = {
          onSave(
            ServiceSettings(
              selectUserApps,
              selectSystemApps,
              excludedApps,
              manuallySelectedApps,
              serviceDuration,
              killAllOnRamLimit,
              killAllRamThreshold,
              killAppOnRamLimit,
              killAppRamThreshold,
              enableRules,
              disableRules,
            ),
          )
        },
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)
        .verticalScroll(rememberScrollState()),
    ) {
      com.yn.shappky.ui.activities.serviceCustomization.sections.ServiceCustomizationSelectAppsSection(
        selectUserApps = selectUserApps,
        onSelectUserAppsChange = { selectUserApps = it },
        selectSystemApps = selectSystemApps,
        onSelectSystemAppsChange = { selectSystemApps = it },
        excludedApps = excludedApps,
        onExcludedAppsChange = { excludedApps = it },
        manuallySelectedApps = manuallySelectedApps,
        onManuallySelectedAppsChange = { manuallySelectedApps = it },
      )

      com.yn.shappky.ui.activities.serviceCustomization.sections.ServiceCustomizationRun(
        serviceDuration = serviceDuration,
        onServiceDurationChange = { serviceDuration = it },
      )

      com.yn.shappky.ui.activities.serviceCustomization.sections.ServiceCustomizationKillWhenReachRam(
        killAllOnRamLimit = killAllOnRamLimit,
        onKillAllOnRamLimitChange = { killAllOnRamLimit = it },
        killAllRamThreshold = killAllRamThreshold,
        onKillAllRamThresholdChange = { killAllRamThreshold = it },
        killAppOnRamLimit = killAppOnRamLimit,
        onKillAppOnRamLimitChange = { killAppOnRamLimit = it },
        killAppRamThreshold = killAppRamThreshold,
        onKillAppRamThresholdChange = { killAppRamThreshold = it },
      )

      com.yn.shappky.ui.activities.serviceCustomization.sections.ServiceCustomizationEnableTriggers(
        enableRules = enableRules,
        onEnableRulesChange = { enableRules = it },
      )

      com.yn.shappky.ui.activities.serviceCustomization.sections.ServiceCustomizationDisableTriggers(
        disableRules = disableRules,
        onDisableRulesChange = { disableRules = it },
      )
    }
  }
}
