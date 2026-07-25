package com.yassernull.shappky.ui.activities.serviceCustomization

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.DisableTriggerManager
import com.yassernull.shappky.core.managers.EnableTriggerManager

object ServiceCustomizationActions {

  fun loadSettings(activity: Activity): ServiceSettings {
    val sharedPreferences = activity.getSharedPreferences(ServiceCustomizationActivity.PREFERENCES_NAME, Context.MODE_PRIVATE)
    val selectUserApps = sharedPreferences.getBoolean("service_select_user_apps", true)
    val selectSystemApps = sharedPreferences.getBoolean("service_select_system_apps", false)
    val excludedApps = sharedPreferences.getStringSet("service_excluded_apps", emptySet()) ?: emptySet()
    val manuallySelectedApps = sharedPreferences.getStringSet("service_manually_selected_apps", emptySet()) ?: emptySet()
    val serviceDuration = sharedPreferences.getLong("service_duration", 18000L)
    val killAllOnRamLimit = sharedPreferences.getBoolean("service_kill_all_on_ram_limit", false)
    val killAllRamThreshold = sharedPreferences.getInt("service_kill_all_ram_threshold", 0)
    val killAppOnRamLimit = sharedPreferences.getBoolean("service_kill_app_on_ram_limit", false)
    val killAppRamThreshold = sharedPreferences.getInt("service_kill_app_ram_threshold", 0)
    val enableRules = EnableTriggerManager.getEnableRules(activity)
    val disableRules = DisableTriggerManager.getDisableRules(activity)

    return ServiceSettings(
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
    )
  }

  fun saveSettings(activity: Activity, settings: ServiceSettings) {
    val sharedPreferences = activity.getSharedPreferences(ServiceCustomizationActivity.PREFERENCES_NAME, Context.MODE_PRIVATE)
    sharedPreferences.edit()
      .putBoolean("service_select_user_apps", settings.selectUserApps)
      .putBoolean("service_select_system_apps", settings.selectSystemApps)
      .putStringSet("service_excluded_apps", settings.excludedApps)
      .putStringSet("service_manually_selected_apps", settings.manuallySelectedApps)
      .putLong("service_duration", settings.serviceDuration)
      .putBoolean("service_kill_all_on_ram_limit", settings.killAllOnRamLimit)
      .putInt("service_kill_all_ram_threshold", settings.killAllRamThreshold)
      .putBoolean("service_kill_app_on_ram_limit", settings.killAppOnRamLimit)
      .putInt("service_kill_app_ram_threshold", settings.killAppRamThreshold)
      .apply()

    EnableTriggerManager.saveEnableRules(activity, settings.enableRules)
    DisableTriggerManager.saveDisableRules(activity, settings.disableRules)

    Toast.makeText(activity, activity.getString(R.string.save), Toast.LENGTH_SHORT).show()
    activity.finish()
  }
}
