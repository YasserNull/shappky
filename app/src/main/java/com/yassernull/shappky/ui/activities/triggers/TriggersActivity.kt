package com.yassernull.shappky.ui.activities.triggers

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.BackgroundAppManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.data.models.TriggerModel
import com.yassernull.shappky.ui.activities.triggers.events.handleOnCreate
import com.yassernull.shappky.ui.activities.triggers.events.handleOnDestroy
import com.yassernull.shappky.ui.activities.triggers.events.handleOnResume
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TriggersActivity : ComponentActivity() {
  internal val triggers = mutableStateListOf<TriggerModel>()
  internal var triggerToDelete by mutableStateOf<TriggerModel?>(null)

  internal val handler = Handler(Looper.getMainLooper())
  internal val executor: ExecutorService = Executors.newSingleThreadExecutor()
  internal lateinit var shellManager: ShellManager
  internal lateinit var appManager: BackgroundAppManager

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(com.yassernull.shappky.core.managers.LocaleManager.getLanguageContext(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleOnCreate(savedInstanceState)
  }

  override fun onResume() {
    super.onResume()
    handleOnResume()
  }

  override fun onDestroy() {
    super.onDestroy()
    handleOnDestroy()
  }

  internal fun loadTriggers() {
    triggers.clear()
    triggers.addAll(TriggerManager.getTriggers(this))
  }

  internal fun executeTrigger(trigger: TriggerModel) {
    if (!shellManager.hasAnyShellPermission()) {
      shellManager.checkShellPermissions()
      return
    }

    appManager.loadBackgroundApps { runningApps ->
      val toKill = runningApps.filter { app ->
        val matchesUser = !app.isSystemApp && !app.isPersistentApp && trigger.selectUserApps
        val matchesSystem = app.isSystemApp && trigger.selectSystemApps
        val matchesPersistent = app.isPersistentApp && trigger.selectPersistentApps
        val matchesManual = trigger.manuallySelectedApps.contains(app.packageName)
        val isExcluded = trigger.excludedApps.contains(app.packageName)

        (matchesUser || matchesSystem || matchesPersistent || matchesManual) && !isExcluded && !app.isProtected
      }.map { it.packageName }

      if (toKill.isNotEmpty()) {
        appManager.killPackages(toKill, {
          val totalKb = runningApps.filter { toKill.contains(it.packageName) }.sumOf { it.ramKb }
          val freedText = getString(R.string.free_up_memory, appManager.formatMemorySize(totalKb))
          com.yassernull.shappky.utils.NotificationUtils.showTriggerFreedMemoryNotification(this@TriggersActivity, trigger.name, freedText)
        }, showToast = false)
      } else {
        android.widget.Toast.makeText(this@TriggersActivity, getString(R.string.no_apps_to_kill), android.widget.Toast.LENGTH_SHORT).show()
      }
    }
  }

  internal fun deleteTrigger(trigger: TriggerModel) {
    triggerToDelete = trigger
  }
}
