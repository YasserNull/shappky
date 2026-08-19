package com.yassernull.shappky.providers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.BackgroundAppManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.core.managers.TriggerManager
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import com.yassernull.shappky.core.preferences.WidgetPreferences
import com.yassernull.shappky.data.models.AppModel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class ShappkyWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    for (appWidgetId in appWidgetIds) {
      if (prefs.contains(WidgetPreferences.getTriggerIdKey(appWidgetId))) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
      } else {
        val views = RemoteViews(context.packageName, R.layout.shappky_widget)
        val defaultBgColor = context.getColor(R.color.control_blue)
        views.setInt(R.id.widget_background, "setColorFilter", defaultBgColor)
        views.setFloat(R.id.widget_background, "setAlpha", 1.0f)
        views.setInt(R.id.widget_icon, "setColorFilter", Color.WHITE)
        views.setViewVisibility(R.id.widget_edit_icon, android.view.View.GONE)

        val density = context.resources.displayMetrics.density
        val bgPaddingPx = ((60 - 48) / 2f * density).toInt()
        val iconPaddingPx = ((60 - 50) / 2f * density).toInt()
        views.setViewPadding(R.id.widget_background, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
        views.setViewPadding(R.id.widget_edit_icon, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
        views.setViewPadding(R.id.widget_icon, iconPaddingPx, iconPaddingPx, iconPaddingPx, iconPaddingPx)

        val configIntent = Intent(context, com.yassernull.shappky.ui.activities.widgetConfig.WidgetConfigActivity::class.java).apply {
          putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
          context,
          appWidgetId,
          configIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        views.setOnClickPendingIntent(R.id.widget_edit_icon, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
      }
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (intent.action == ACTION_WIDGET_CLICK) {
      val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
      if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
        val pendingResult = goAsync()
        val executor = Executors.newSingleThreadExecutor()
        if (!executor.isShutdown) {
          executor.execute {
            try {
              executeWidgetTrigger(context, appWidgetId)
            } finally {
              pendingResult.finish()
              executor.shutdown()
            }
          }
        }
      }
    }
  }

  private fun executeWidgetTrigger(context: Context, appWidgetId: Int) {
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val triggerId = prefs.getString(WidgetPreferences.getTriggerIdKey(appWidgetId), "") ?: ""

    val triggers = TriggerManager.getTriggers(context)
    val trigger = triggers.firstOrNull { it.id == triggerId } ?: triggers.firstOrNull()

    if (trigger == null) {
      val handler = Handler(Looper.getMainLooper())
      handler.post {
        Toast.makeText(context, context.getString(R.string.please_select_trigger), Toast.LENGTH_SHORT).show()
      }
      return
    }

    val handler = Handler(Looper.getMainLooper())
    val shellExecutor = Executors.newSingleThreadExecutor()
    val shellManager = ShellManager(context, handler, shellExecutor)
    shellManager.checkShellPermissions()

    if (!shellManager.hasAnyShellPermission()) {
      handler.post {
        Toast.makeText(context, context.getString(R.string.shell_permission_required), Toast.LENGTH_LONG).show()
      }
      shellExecutor.shutdown()
      return
    }

    val appManager = BackgroundAppManager(context, handler, shellExecutor, shellManager)
    val latch = CountDownLatch(1)
    var loadedApps = emptyList<AppModel>()

    appManager.loadBackgroundApps { result ->
      loadedApps = result
      latch.countDown()
    }
    latch.await()

    val toKill = loadedApps.filter { app ->
      val matchesUser = !app.isSystemApp && !app.isPersistentApp && trigger.selectUserApps
      val matchesSystem = app.isSystemApp && trigger.selectSystemApps
      val matchesPersistent = app.isPersistentApp && trigger.selectPersistentApps
      val matchesManual = trigger.manuallySelectedApps.contains(app.packageName)
      val isExcluded = trigger.excludedApps.contains(app.packageName)

      (matchesUser || matchesSystem || matchesPersistent || matchesManual) && !isExcluded && !app.isProtected
    }

    if (toKill.isNotEmpty()) {
      val totalKb = toKill.sumOf { it.ramKb }
      val packageNamesToKill = toKill.map { it.packageName }
      val shouldKillAll = trigger.selectSystemApps || trigger.selectUserApps
      val killLatch = CountDownLatch(1)

      appManager.killPackages(packageNamesToKill, {
        com.yassernull.shappky.core.managers.KillTracker.markKilledAll(packageNamesToKill)
        killLatch.countDown()
      }, showToast = false, appendKillAll = shouldKillAll)
      killLatch.await()

      val freedText = context.getString(R.string.free_up_memory, appManager.formatMemorySize(totalKb))
      handler.post {
        Toast.makeText(context, freedText, Toast.LENGTH_SHORT).show()
      }
      com.yassernull.shappky.utils.NotificationUtils.showTriggerFreedMemoryNotification(context, trigger.name, freedText)
    } else {
      handler.post {
        Toast.makeText(context, context.getString(R.string.no_apps_to_kill), Toast.LENGTH_SHORT).show()
      }
    }

    shellExecutor.shutdown()

    val appWidgetManager = AppWidgetManager.getInstance(context)
    updateAppWidget(context, appWidgetManager, appWidgetId)
  }

  companion object {
    const val ACTION_WIDGET_CLICK = "com.yassernull.shappky.ACTION_WIDGET_CLICK"

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
      val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      val iconColor = prefs.getInt(WidgetPreferences.getIconColorKey(appWidgetId), Color.WHITE)
      val defaultBgColor = context.getColor(R.color.control_blue)
      val bgColor = prefs.getInt(WidgetPreferences.getBgColorKey(appWidgetId), defaultBgColor)
      val bgSize = prefs.getInt(WidgetPreferences.getBgSizeKey(appWidgetId), 48)
      val iconSize = 50

      val views = RemoteViews(context.packageName, R.layout.shappky_widget)
      views.setInt(R.id.widget_background, "setColorFilter", bgColor)
      views.setFloat(R.id.widget_background, "setAlpha", 1.0f)
      views.setInt(R.id.widget_icon, "setColorFilter", iconColor)
      views.setViewVisibility(R.id.widget_edit_icon, android.view.View.GONE)

      val density = context.resources.displayMetrics.density
      val bgPaddingPx = ((60 - bgSize) / 2f * density).toInt()
      val iconPaddingPx = ((60 - iconSize) / 2f * density).toInt()
      views.setViewPadding(R.id.widget_background, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
      views.setViewPadding(R.id.widget_edit_icon, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
      views.setViewPadding(R.id.widget_icon, iconPaddingPx, iconPaddingPx, iconPaddingPx, iconPaddingPx)

      val showLabel = prefs.getBoolean(WidgetPreferences.getShowLabelKey(appWidgetId), true)
      if (showLabel) {
        val triggerId = prefs.getString(WidgetPreferences.getTriggerIdKey(appWidgetId), "") ?: ""
        val widgetTriggers = TriggerManager.getTriggers(context)
        val triggerName = widgetTriggers.firstOrNull { it.id == triggerId }?.name
          ?: widgetTriggers.firstOrNull { it.rules.isEmpty() }?.name
          ?: ""
        views.setViewVisibility(R.id.widget_label, android.view.View.VISIBLE)
        views.setTextViewText(R.id.widget_label, triggerName)
      } else {
        views.setViewVisibility(R.id.widget_label, android.view.View.GONE)
      }

      val clickIntent = Intent(context, ShappkyWidgetProvider::class.java).apply {
        action = ACTION_WIDGET_CLICK
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      }
      val pendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        clickIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: android.os.Bundle,
  ) {
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    if (prefs.contains(WidgetPreferences.getTriggerIdKey(appWidgetId))) {
      val views = RemoteViews(context.packageName, R.layout.shappky_widget)
      val iconColor = prefs.getInt(WidgetPreferences.getIconColorKey(appWidgetId), Color.WHITE)
      val defaultBgColor = context.getColor(R.color.control_blue)
      val bgColor = prefs.getInt(WidgetPreferences.getBgColorKey(appWidgetId), defaultBgColor)
      val bgSize = prefs.getInt(WidgetPreferences.getBgSizeKey(appWidgetId), 48)
      val iconSize = 50

      views.setInt(R.id.widget_background, "setColorFilter", bgColor)
      views.setFloat(R.id.widget_background, "setAlpha", 1.0f)
      views.setInt(R.id.widget_icon, "setColorFilter", iconColor)
      views.setViewVisibility(R.id.widget_edit_icon, android.view.View.GONE)

      val density = context.resources.displayMetrics.density
      val bgPaddingPx = ((60 - bgSize) / 2f * density).toInt()
      val iconPaddingPx = ((60 - iconSize) / 2f * density).toInt()
      views.setViewPadding(R.id.widget_background, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
      views.setViewPadding(R.id.widget_edit_icon, bgPaddingPx, bgPaddingPx, bgPaddingPx, bgPaddingPx)
      views.setViewPadding(R.id.widget_icon, iconPaddingPx, iconPaddingPx, iconPaddingPx, iconPaddingPx)

      val showLabel = prefs.getBoolean(WidgetPreferences.getShowLabelKey(appWidgetId), true)
      if (showLabel) {
        val triggerId = prefs.getString(WidgetPreferences.getTriggerIdKey(appWidgetId), "") ?: ""
        val widgetTriggers = TriggerManager.getTriggers(context)
        val triggerName = widgetTriggers.firstOrNull { it.id == triggerId }?.name
          ?: widgetTriggers.firstOrNull { it.rules.isEmpty() }?.name
          ?: ""
        views.setViewVisibility(R.id.widget_label, android.view.View.VISIBLE)
        views.setTextViewText(R.id.widget_label, triggerName)
      } else {
        views.setViewVisibility(R.id.widget_label, android.view.View.GONE)
      }

      val clickIntent = Intent(context, ShappkyWidgetProvider::class.java).apply {
        action = ACTION_WIDGET_CLICK
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      }
      val pendingIntentClick = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        clickIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      views.setOnClickPendingIntent(R.id.widget_root, pendingIntentClick)

      val configIntent = Intent(context, com.yassernull.shappky.ui.activities.widgetConfig.WidgetConfigActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId,
        configIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      views.setOnClickPendingIntent(R.id.widget_edit_icon, pendingIntent)

      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }
}
