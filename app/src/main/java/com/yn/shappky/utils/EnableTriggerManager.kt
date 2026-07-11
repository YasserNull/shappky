package com.yn.shappky.utils

import android.content.Context
import com.yn.shappky.data.models.RuleType
import com.yn.shappky.data.models.TriggerRule
import org.json.JSONArray
import org.json.JSONObject

object EnableTriggerManager {
  private const val PREFS_NAME = "AppPreferences"
  private const val KEY_ENABLE_RULES = "enable_rules_list"

  fun getEnableRules(context: Context): List<TriggerRule> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(KEY_ENABLE_RULES, null) ?: return emptyList()
    val rules = mutableListOf<TriggerRule>()
    try {
      val rulesArray = JSONArray(jsonStr)
      for (j in 0 until rulesArray.length()) {
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
          timeHour = ruleObj.optInt("timeHour", 0),
          timeMinute = ruleObj.optInt("timeMinute", 0),
          inactivityDurationMinutes = ruleObj.optInt("inactivityDurationMinutes", 0),
          selectedServices = selectedServices,
        )
        rules.add(rule)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return rules
  }

  fun saveEnableRules(context: Context, rules: List<TriggerRule>) {
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
      ruleObj.put("timeHour", rule.timeHour)
      ruleObj.put("timeMinute", rule.timeMinute)
      ruleObj.put("inactivityDurationMinutes", rule.inactivityDurationMinutes)
      val servicesArray = JSONArray()
      rule.selectedServices.forEach { servicesArray.put(it) }
      ruleObj.put("selectedServices", servicesArray)
      rulesArray.put(ruleObj)
    }
    prefs.edit().putString(KEY_ENABLE_RULES, rulesArray.toString()).apply()
  }
}
