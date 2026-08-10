package com.yassernull.shappky.data.models

enum class RuleType {
  APP_OPENED,
  RAM_LIMIT_REACHED,
  APP_RAM_EXCEEDED,
  PHONE_SLEEP,
  PHONE_WAKE,
  SPECIFIC_TIME,
  APP_INACTIVITY,
  SERVICE_STATE_CHANGED,
  KILL_OLDEST_APP,
  APP_RESUMED,
  APP_CLOSED,
  APP_KILLED_MANUALLY,
  APP_BACKGROUND_STARTED,
}

data class TriggerRule(
  val id: String,
  val type: RuleType,
  val appPackages: Set<String> = emptySet(),
  val ramThresholdMb: Int = 0,
  val sleepDurationMinutes: Int = 0,
  val timeHour: Int = 0,
  val timeMinute: Int = 0,
  val inactivityDurationMinutes: Int = 0,
  val selectedServices: Set<String> = emptySet(),
)

data class TriggerModel(
  val id: String,
  val name: String,
  val selectUserApps: Boolean = false,
  val selectSystemApps: Boolean = false,
  val selectPersistentApps: Boolean = false,
  val excludedApps: Set<String> = emptySet(),
  val manuallySelectedApps: Set<String> = emptySet(),
  val rules: List<TriggerRule> = emptyList(),
  val enableRules: List<TriggerRule> = emptyList(),
  val disableRules: List<TriggerRule> = emptyList(),
  val isEnabled: Boolean = true,
  val serviceDuration: Long = 0,
)
