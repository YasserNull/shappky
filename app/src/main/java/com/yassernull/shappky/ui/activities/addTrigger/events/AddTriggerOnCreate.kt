package com.yassernull.shappky.ui.activities.addTrigger.events

import android.os.Bundle
import androidx.activity.compose.setContent
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.core.preferences.applyDynamicColorsFromPreferences
import com.yassernull.shappky.core.preferences.applyThemeFromPreferences
import com.yassernull.shappky.ui.activities.addTrigger.AddTriggerActions
import com.yassernull.shappky.ui.activities.addTrigger.AddTriggerActivity
import com.yassernull.shappky.ui.activities.addTrigger.AddTriggerContent
import com.yassernull.shappky.ui.components.applySystemBars
import com.yassernull.shappky.ui.theme.AppTheme

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
    AppTheme {
      AddTriggerContent(
        initialTrigger = initialTrigger,
        triggerCount = triggerCount,
        onSave = { AddTriggerActions.saveTrigger(it, this@handleOnCreate) },
        onBack = { finish() },
      )
    }
  }
}
