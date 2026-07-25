package com.yassernull.shappky.ui.activities.triggers.events

import android.os.Bundle
import androidx.activity.compose.setContent
import com.yassernull.shappky.core.managers.BackgroundAppManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.core.preferences.applyDynamicColorsFromPreferences
import com.yassernull.shappky.core.preferences.applyThemeFromPreferences
import com.yassernull.shappky.ui.activities.triggers.TriggersActivity
import com.yassernull.shappky.ui.activities.triggers.TriggersContent
import com.yassernull.shappky.ui.components.applySystemBars
import com.yassernull.shappky.ui.dialogs.DeleteTriggerDialog
import com.yassernull.shappky.ui.theme.AppTheme

fun TriggersActivity.handleOnCreate(savedInstanceState: Bundle?) {
  applyThemeFromPreferences()
  applyDynamicColorsFromPreferences()
  applySystemBars()

  shellManager = ShellManager(this, handler, executor)
  appManager = BackgroundAppManager(this, handler, executor, shellManager)

  shellManager.checkShellPermissions()

  setContent {
    AppTheme {
      TriggersContent(
        triggers = triggers,
        onBack = { finish() },
        onExecute = { executeTrigger(it) },
        onDelete = { deleteTrigger(it) },
        onToggleState = { trigger, isEnabled ->
          val updated = trigger.copy(isEnabled = isEnabled)
          TriggerManager.addOrUpdateTrigger(this@handleOnCreate, updated)
          loadTriggers()
          com.yassernull.shappky.core.managers.TriggerServiceManager.updateTriggerServiceState(this@handleOnCreate)
        },
      )

      if (triggerToDelete != null) {
        DeleteTriggerDialog(
          trigger = triggerToDelete!!,
          onConfirm = { trigger ->
            TriggerManager.deleteTrigger(this@handleOnCreate, trigger.id)
            loadTriggers()
            com.yassernull.shappky.core.managers.TriggerServiceManager.updateTriggerServiceState(this@handleOnCreate)
            triggerToDelete = null
          },
          onDismiss = { triggerToDelete = null },
        )
      }
    }
  }
}
