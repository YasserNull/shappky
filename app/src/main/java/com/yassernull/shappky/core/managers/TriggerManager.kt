package com.yassernull.shappky.core.managers

import android.content.Context
import android.util.Log
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.core.preferences.TriggerPreferences
import com.yassernull.shappky.data.models.RuleType
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.data.models.TriggerRule
import org.json.JSONArray
import org.json.JSONObject

object TriggerManager {

  fun getTriggers(context: Context): List<TriggerModel> {
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val jsonStr = prefs.getString(TriggerPreferences.KEY_TRIGGERS, null) ?: return emptyList()
    val list = mutableListOf<TriggerModel>()
    try {
      val jsonArray = JSONArray(jsonStr)
      for (i in 0 until jsonArray.length()) {
        try {
          val obj = jsonArray.getJSONObject(i)
          val excluded = mutableSetOf<String>()
          val excludedArray = obj.optJSONArray("excludedApps")
          if (excludedArray != null) {
            for (j in 0 until excludedArray.length()) {
              excluded.add(excludedArray.getString(j))
            }
          }

          val manual = mutableSetOf<String>()
          val manualArray = obj.optJSONArray("manuallySelectedApps")
          if (manualArray != null) {
            for (j in 0 until manualArray.length()) {
              manual.add(manualArray.getString(j))
            }
          }

          val rules = mutableListOf<TriggerRule>()
          val rulesArray = obj.optJSONArray("rules")
          if (rulesArray != null) {
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
              rules.add(
                TriggerRule(
                  id = ruleObj.getString("id"),
                  type = RuleType.valueOf(ruleObj.getString("type")),
                  appPackages = appPackages,
                  ramThresholdMb = ruleObj.optInt("ramThresholdMb", 0),
                  sleepDurationMinutes = ruleObj.optInt("sleepDurationMinutes", 0),
                  timeHour = ruleObj.optInt("timeHour", 0),
                  timeMinute = ruleObj.optInt("timeMinute", 0),
                  inactivityDurationMinutes = ruleObj.optInt("inactivityDurationMinutes", 0),
                  selectedServices = selectedServices,
                ),
              )
            }
          }

          list.add(
            TriggerModel(
              id = obj.getString("id"),
              name = obj.getString("name"),
              selectUserApps = obj.optBoolean("selectUserApps", false),
              selectSystemApps = obj.optBoolean("selectSystemApps", false),
              selectPersistentApps = obj.optBoolean("selectPersistentApps", false),
              excludedApps = excluded,
              manuallySelectedApps = manual,
              rules = rules,
              isEnabled = obj.optBoolean("isEnabled", true),
            ),
          )
        } catch (e: Exception) {
          Log.w("TriggerManager", "Skipping corrupted trigger entry at index $i", e)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return list
  }

  fun saveTriggers(context: Context, triggers: List<TriggerModel>) {
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val jsonArray = JSONArray()
    for (trigger in triggers) {
      val obj = JSONObject()
      obj.put("id", trigger.id)
      obj.put("name", trigger.name)
      obj.put("selectUserApps", trigger.selectUserApps)
      obj.put("selectSystemApps", trigger.selectSystemApps)
      obj.put("selectPersistentApps", trigger.selectPersistentApps)
      obj.put("isEnabled", trigger.isEnabled)

      val excludedArray = JSONArray()
      trigger.excludedApps.forEach { excludedArray.put(it) }
      obj.put("excludedApps", excludedArray)

      val manualArray = JSONArray()
      trigger.manuallySelectedApps.forEach { manualArray.put(it) }
      obj.put("manuallySelectedApps", manualArray)

      val rulesArray = JSONArray()
      for (rule in trigger.rules) {
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
      obj.put("rules", rulesArray)

      jsonArray.put(obj)
    }
    prefs.edit().putString(TriggerPreferences.KEY_TRIGGERS, jsonArray.toString()).apply()
  }

  fun addOrUpdateTrigger(context: Context, trigger: TriggerModel) {
    val triggers = getTriggers(context).toMutableList()
    val index = triggers.indexOfFirst { it.id == trigger.id }
    if (index >= 0) {
      triggers[index] = trigger
    } else {
      triggers.add(trigger)
    }
    saveTriggers(context, triggers)
  }

  fun deleteTrigger(context: Context, id: String) {
    val triggers = getTriggers(context).filter { it.id != id }
    saveTriggers(context, triggers)
  }
}
