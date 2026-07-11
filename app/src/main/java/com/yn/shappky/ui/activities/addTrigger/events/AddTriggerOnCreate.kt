package com.yn.shappky.ui.activities.addTrigger.events

import android.os.Bundle
import androidx.activity.compose.setContent
import com.yn.shappky.core.preferences.ShappkyTheme
import com.yn.shappky.core.preferences.applyDynamicColorsFromPreferences
import com.yn.shappky.core.preferences.applySystemBars
import com.yn.shappky.core.preferences.applyThemeFromPreferences
import com.yn.shappky.ui.activities.addTrigger.AddTriggerActions
import com.yn.shappky.ui.activities.addTrigger.AddTriggerActivity
import com.yn.shappky.ui.activities.addTrigger.AddTriggerContent
import com.yn.shappky.utils.TriggerManager

fun AddTriggerActivity.handleOnCreate(savedInstanceState: Bundle?) {
  applyThemeFromPreferences()
  applyDynamicColorsFromPreferences()
  applySystemBars()

  triggerId = intent.getStringExtra(AddTriggerActivity.EXTRA_TRIGGER_ID)
  val triggers = TriggerManager.getTriggers(this)
  triggerCount = triggers.size

  if (triggerId != null) {
    initialTrigger = triggers.find { it.id == triggerId }
  }

  setContent {
    ShappkyTheme {
      AddTriggerContent(
        initialTrigger = initialTrigger,
        triggerCount = triggerCount,
        onSave = { AddTriggerActions.saveTrigger(it, this@handleOnCreate) },
        onBack = { finish() },
      )
    }
  }
}
