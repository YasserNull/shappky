package com.yassernull.shappky.ui.activities.main

import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.services.ShappkyService

object MainActions {
  fun onOpenSettings(context: android.content.Context) {
    context.startActivity(android.content.Intent(context, com.yassernull.shappky.ui.activities.settings.SettingsActivity::class.java))
  }

  fun onOpenDonate(context: android.content.Context) {
    try {
      context.startActivity(
        android.content.Intent(
          android.content.Intent.ACTION_VIEW,
          android.net.Uri.parse(context.getString(R.string.donate_url)),
        ),
      )
    } catch (_: Exception) {}
  }

  fun onOpenTriggers(context: android.content.Context) {
    context.startActivity(android.content.Intent(context, com.yassernull.shappky.ui.activities.triggers.TriggersActivity::class.java))
  }

  fun onToggleService(
    activity: ComponentActivity,
    start: Boolean,
    shellManager: ShellManager,
  ) {
    if (!PermissionHandler.hasNotificationPermission(activity)) {
      PermissionHandler.checkAndRequestNotificationPermission(activity)
    } else {
      if (start) {
        if (!shellManager.hasAnyShellPermission()) {
          shellManager.checkShellPermissions()
          android.widget.Toast.makeText(activity, activity.getString(R.string.shell_permission_required), android.widget.Toast.LENGTH_SHORT).show()
        } else {
          ContextCompat.startForegroundService(
            activity,
            android.content.Intent(activity, ShappkyService::class.java),
          )
        }
      } else {
        activity.stopService(android.content.Intent(activity, ShappkyService::class.java))
      }
    }
  }
}
