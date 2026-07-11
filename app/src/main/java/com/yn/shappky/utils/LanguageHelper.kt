package com.yn.shappky.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.yn.shappky.core.preferences.KEY_LANGUAGE
import com.yn.shappky.core.preferences.PREFERENCES_NAME
import com.yn.shappky.providers.ShappkyListWidgetProvider
import com.yn.shappky.providers.ShappkyWidgetProvider

object LanguageHelper {
  fun getLanguageContext(newBase: Context): Context {
    val prefs = newBase.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val language = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
    return if (language != "system") {
      val locale = java.util.Locale.forLanguageTag(language)
      java.util.Locale.setDefault(locale)
      val config = android.content.res.Configuration(newBase.resources.configuration)
      config.setLocale(locale)
      newBase.createConfigurationContext(config)
    } else {
      newBase
    }
  }

  fun updateLauncherComponent(context: Context, language: String) {
    val pm = context.packageManager
    val englishAlias = ComponentName(context, "com.yn.shappky.LauncherEnglish")
    val arabicAlias = ComponentName(context, "com.yn.shappky.LauncherArabic")

    val targetArEnabled = if (language == "system") {
      java.util.Locale.getDefault().language == "ar"
    } else {
      language == "ar"
    }

    val currentArState = pm.getComponentEnabledSetting(arabicAlias)
    val isArCurrentlyEnabled = currentArState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    val currentEnState = pm.getComponentEnabledSetting(englishAlias)
    val isEnCurrentlyEnabled = currentEnState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || currentEnState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT

    if (targetArEnabled) {
      if (!isArCurrentlyEnabled || isEnCurrentlyEnabled) {
        pm.setComponentEnabledSetting(
          arabicAlias,
          PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
          PackageManager.DONT_KILL_APP,
        )
        pm.setComponentEnabledSetting(
          englishAlias,
          PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
          PackageManager.DONT_KILL_APP,
        )
      }
    } else {
      if (!isEnCurrentlyEnabled || isArCurrentlyEnabled) {
        pm.setComponentEnabledSetting(
          englishAlias,
          PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
          PackageManager.DONT_KILL_APP,
        )
        pm.setComponentEnabledSetting(
          arabicAlias,
          PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
          PackageManager.DONT_KILL_APP,
        )
      }
    }
  }

  fun updateAllWidgets(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)

    // Update List Widgets
    val listIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ShappkyListWidgetProvider::class.java))
    for (id in listIds) {
      ShappkyListWidgetProvider.updateAppWidget(context, appWidgetManager, id)
    }
    if (listIds.isNotEmpty()) {
      @Suppress("DEPRECATION")
      appWidgetManager.notifyAppWidgetViewDataChanged(listIds, com.yn.shappky.R.id.widget_list_view)
    }

    // Update Trigger Widgets
    val triggerIds = appWidgetManager.getAppWidgetIds(ComponentName(context, ShappkyWidgetProvider::class.java))
    for (id in triggerIds) {
      ShappkyWidgetProvider.updateAppWidget(context, appWidgetManager, id)
    }
  }

  fun showTriggerFreedMemoryNotification(context: Context, triggerName: String, freedMemoryText: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channelId = "ShappkyTriggerChannel"

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      val channel = android.app.NotificationChannel(
        channelId,
        context.getString(com.yn.shappky.R.string.trigger_channel_name),
        android.app.NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = context.getString(com.yn.shappky.R.string.trigger_channel_desc)
      }
      notificationManager.createNotificationChannel(channel)
    }

    val fullText = "$triggerName: $freedMemoryText"

    val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
      .setContentTitle(context.getString(com.yn.shappky.R.string.trigger_channel_name))
      .setContentText(fullText)
      .setSmallIcon(com.yn.shappky.R.drawable.ic_shappky)
      .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setAutoCancel(false)

    notificationManager.notify(2, builder.build())
  }
}
