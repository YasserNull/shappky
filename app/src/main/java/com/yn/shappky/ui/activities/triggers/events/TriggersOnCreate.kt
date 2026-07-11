package com.yn.shappky.ui.activities.triggers.events

import android.os.Bundle
import androidx.activity.compose.setContent
import com.yn.shappky.core.preferences.ShappkyTheme
import com.yn.shappky.core.preferences.applyDynamicColorsFromPreferences
import com.yn.shappky.core.preferences.applySystemBars
import com.yn.shappky.core.preferences.applyThemeFromPreferences
import com.yn.shappky.ui.activities.triggers.TriggersActivity
import com.yn.shappky.ui.activities.triggers.TriggersContent
import com.yn.shappky.ui.dialogs.DeleteTriggerDialog
import com.yn.shappky.utils.BackgroundAppManager
import com.yn.shappky.utils.ShellManager
import com.yn.shappky.utils.TriggerManager

fun TriggersActivity.handleOnCreate(savedInstanceState: Bundle?) {
  applyThemeFromPreferences()
  applyDynamicColorsFromPreferences()
  applySystemBars()

  shellManager = ShellManager(this, handler, executor)
  appManager = BackgroundAppManager(this, handler, executor, shellManager)

  shellManager.checkShellPermissions()

  setContent {
    ShappkyTheme {
      TriggersContent(
        triggers = triggers,
        onBack = { finish() },
        onExecute = { executeTrigger(it) },
        onDelete = { deleteTrigger(it) },
        onToggleState = { trigger, isEnabled ->
          val updated = trigger.copy(isEnabled = isEnabled)
          TriggerManager.addOrUpdateTrigger(this@handleOnCreate, updated)
          loadTriggers()
          com.yn.shappky.utils.TriggerServiceManager.updateTriggerServiceState(this@handleOnCreate)
        },
      )

      if (triggerToDelete != null) {
        DeleteTriggerDialog(
          trigger = triggerToDelete!!,
          onConfirm = { trigger ->
            TriggerManager.deleteTrigger(this@handleOnCreate, trigger.id)
            loadTriggers()
            com.yn.shappky.utils.TriggerServiceManager.updateTriggerServiceState(this@handleOnCreate)
            triggerToDelete = null
          },
          onDismiss = { triggerToDelete = null },
        )
      }
    }
  }
}
