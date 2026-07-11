package com.yn.shappky.utils

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.yn.shappky.R
import com.yn.shappky.providers.ShappkyListWidgetProvider
import com.yn.shappky.providers.ShappkyWidgetProvider

fun Context.restartApp() {
  packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
  }
  if (this is Activity) {
    finish()
  }
  Runtime.getRuntime().exit(0)
}

@Suppress("DEPRECATION")
fun Context.updateAllWidgets() {
  val appWidgetManager = AppWidgetManager.getInstance(this)
  val iconIds = appWidgetManager.getAppWidgetIds(ComponentName(this, ShappkyWidgetProvider::class.java))
  if (iconIds.isNotEmpty()) {
    val iconIntent = Intent(this, ShappkyWidgetProvider::class.java).apply {
      action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, iconIds)
    }
    sendBroadcast(iconIntent)
  }

  val listIds = appWidgetManager.getAppWidgetIds(ComponentName(this, ShappkyListWidgetProvider::class.java))
  if (listIds.isNotEmpty()) {
    val listIntent = Intent(this, ShappkyListWidgetProvider::class.java).apply {
      action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, listIds)
    }
    sendBroadcast(listIntent)
    listIds.forEach { id ->
      appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_list_view)
    }
  }
}
