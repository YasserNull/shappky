package com.yassernull.shappky.core.managers

import android.content.Context
import android.util.Log
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerRule
import org.json.JSONArray
import org.json.JSONObject

object DisableTriggerManager {
  private const val PREFS_NAME = "AppPreferences"
  private const val KEY_RULES = "disable_rules_list"

  fun getDisableRules(context: Context): List<TriggerRule> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(KEY_RULES, null) ?: return emptyList()
    val rules = mutableListOf<TriggerRule>()
    try {
      val rulesArray = JSONArray(jsonStr)
      for (j in 0 until rulesArray.length()) {
        try {
          val ruleObj = rulesArray.getJSONObject(j)
          val appPackages = mutableSetOf<String>()
          val packagesArray = ruleObj.optJSONArray("appPackages")
          if (packagesArray != null) {
            for (k in 0 until packagesArray.length()) {
              appPackages.add(packagesArray.getString(k))
            }
          }
          val selectedServices = mutableSetOf<String>()
          val servicesArray = ruleObj.optJSONArray("selectedServices")
          if (servicesArray != null) {
            for (k in 0 until servicesArray.length()) {
              selectedServices.add(servicesArray.getString(k))
            }
          }
          val rule = TriggerRule(
            id = ruleObj.getString("id"),
            type = RuleType.valueOf(ruleObj.getString("type")),
            appPackages = appPackages,
            ramThresholdMb = ruleObj.optInt("ramThresholdMb", 0),
            sleepDurationMinutes = ruleObj.optInt("sleepDurationMinutes", 0),
            timeHour = ruleObj.optInt("timeHour", 0),
            timeMinute = ruleObj.optInt("timeMinute", 0),
            inactivityDurationMinutes = ruleObj.optInt("inactivityDurationMinutes", 0),
            selectedServices = selectedServices,
          )
          rules.add(rule)
        } catch (e: Exception) {
          Log.w("DisableTriggerManager", "Skipping corrupted rule entry at index $j", e)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return rules
  }

  fun saveDisableRules(context: Context, rules: List<TriggerRule>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val rulesArray = JSONArray()
    for (rule in rules) {
      val ruleObj = JSONObject()
      ruleObj.put("id", rule.id)
      ruleObj.put("type", rule.type.name)
      val packagesArray = JSONArray()
      rule.appPackages.forEach { packagesArray.put(it) }
      ruleObj.put("appPackages", packagesArray)
      ruleObj.put("ramThresholdMb", rule.ramThresholdMb)
      ruleObj.put("sleepDurationMinutes", rule.sleepDurationMinutes)
      ruleObj.put("timeHour", rule.timeHour)
      ruleObj.put("timeMinute", rule.timeMinute)
      ruleObj.put("inactivityDurationMinutes", rule.inactivityDurationMinutes)
      val servicesArray = JSONArray()
      rule.selectedServices.forEach { servicesArray.put(it) }
      ruleObj.put("selectedServices", servicesArray)
      rulesArray.put(ruleObj)
    }
    prefs.edit().putString(KEY_RULES, rulesArray.toString()).apply()
  }
}
