package com.yn.shappky.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.yn.shappky.R
import com.yn.shappky.data.models.RuleType
import com.yn.shappky.data.models.TriggerRule
import com.yn.shappky.utils.getAppName

@Composable
fun buildRuleSummary(rule: TriggerRule): String {
  val context = LocalContext.current
  return when (rule.type) {
    RuleType.APP_OPENED -> {
      val appNames = rule.appPackages.joinToString(", ") { context.getAppName(it) }
      context.getString(R.string.rule_summary_app_opened, appNames)
    }
    RuleType.APP_RESUMED -> {
      val appNames = rule.appPackages.joinToString(", ") { context.getAppName(it) }
      context.getString(R.string.rule_summary_app_resumed, appNames)
    }
    RuleType.APP_CLOSED -> {
      val appNames = rule.appPackages.joinToString(", ") { context.getAppName(it) }
      context.getString(R.string.rule_summary_app_closed, appNames)
    }
    RuleType.APP_KILLED_MANUALLY -> {
      val appNames = rule.appPackages.joinToString(", ") { context.getAppName(it) }
      context.getString(R.string.rule_summary_app_killed_manually, appNames)
    }
    RuleType.RAM_LIMIT_REACHED -> {
      context.getString(R.string.rule_summary_ram_limit, rule.ramThresholdMb)
    }
    RuleType.APP_RAM_EXCEEDED -> {
      val appNames = rule.appPackages.joinToString(", ") { context.getAppName(it) }
      context.getString(R.string.rule_summary_app_ram, appNames, rule.ramThresholdMb)
    }
    RuleType.PHONE_SLEEP -> {
      context.getString(R.string.rule_summary_phone_sleep, rule.sleepDurationMinutes)
    }
    RuleType.PHONE_WAKE -> {
      context.getString(R.string.rule_summary_phone_wake)
    }
    RuleType.SPECIFIC_TIME -> {
      context.getString(R.string.rule_summary_specific_time, rule.timeHour, rule.timeMinute)
    }
    RuleType.APP_INACTIVITY -> {
      val appNames = rule.appPackages.joinToString(", ") { context.getAppName(it) }
      context.getString(R.string.rule_summary_app_inactivity, appNames, rule.inactivityDurationMinutes)
    }
    RuleType.SERVICE_STATE_CHANGED -> {
      val serviceNames = rule.selectedServices.joinToString(", ") { key ->
        val resId = when (key) {
          "wifi" -> R.string.service_wifi
          "bluetooth" -> R.string.service_bluetooth
          "mobile_data" -> R.string.service_mobile_data
          "airplane_mode" -> R.string.service_airplane_mode
          "gps" -> R.string.service_gps
          "hotspot" -> R.string.service_hotspot
          "dnd" -> R.string.service_dnd
          "nfc" -> R.string.service_nfc
          else -> R.string.rule_service_state
        }
        context.getString(resId)
      }
      context.getString(R.string.rule_summary_service_state, serviceNames)
    }
    RuleType.KILL_OLDEST_APP -> {
      context.getString(R.string.rule_summary_kill_oldest)
    }
  }
}
