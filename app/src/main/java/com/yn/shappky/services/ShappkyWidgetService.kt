package com.yn.shappky.services

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.HandlerThread
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.yn.shappky.R
import com.yn.shappky.core.preferences.AppsListPreferences
import com.yn.shappky.core.preferences.KEY_THEME
import com.yn.shappky.core.preferences.PREFERENCES_NAME
import com.yn.shappky.core.preferences.WidgetPreferences
import com.yn.shappky.data.models.AppModel
import com.yn.shappky.utils.BackgroundAppManager
import com.yn.shappky.utils.ShellManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ShappkyWidgetService : RemoteViewsService() {
  override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = ShappkyWidgetFactory(applicationContext, intent)
}

class ShappkyWidgetFactory(
  private val context: Context,
  private val intent: Intent,
) : RemoteViewsService.RemoteViewsFactory {

  private var appsList = listOf<AppModel>()
  private var appManager: BackgroundAppManager? = null
  private var executor: ExecutorService? = null
  private var thread: HandlerThread? = null
  private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

  override fun onCreate() {
    appWidgetId = intent.getIntExtra(
      AppWidgetManager.EXTRA_APPWIDGET_ID,
      AppWidgetManager.INVALID_APPWIDGET_ID,
    )
    executor = Executors.newSingleThreadExecutor()
    thread = HandlerThread("WidgetBackgroundThread").apply { start() }
    val handler = Handler(thread!!.looper)
    val shellManager = ShellManager(context, handler, executor!!)
    appManager = BackgroundAppManager(context, handler, executor!!, shellManager)
  }

  override fun onDataSetChanged() {
    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val manager = appManager ?: return

    val showUserApps = prefs.getBoolean(WidgetPreferences.getListShowUserAppsKey(appWidgetId), prefs.getBoolean(AppsListPreferences.KEY_SHOW_USER_APPS, true))
    val showSystemApps = prefs.getBoolean(WidgetPreferences.getListShowSystemAppsKey(appWidgetId), prefs.getBoolean(AppsListPreferences.KEY_SHOW_SYSTEM_APPS, false))
    val showPersistentApps = prefs.getBoolean(WidgetPreferences.getListShowPersistentAppsKey(appWidgetId), prefs.getBoolean(AppsListPreferences.KEY_SHOW_PERSISTENT_APPS, false))
    val showProtectedApps = prefs.getBoolean(WidgetPreferences.getListShowProtectedAppsKey(appWidgetId), prefs.getBoolean(AppsListPreferences.KEY_SHOW_PROTECTED_APPS, true))

    manager.setShowUserApps(showUserApps)
    manager.setShowSystemApps(showSystemApps)
    manager.setShowPersistentApps(showPersistentApps)
    manager.setShowProtectedApps(showProtectedApps)

    val latch = CountDownLatch(1)
    var loadedApps = listOf<AppModel>()
    manager.loadBackgroundApps { apps ->
      loadedApps = apps
      latch.countDown()
    }
    try {
      latch.await(8, TimeUnit.SECONDS)
    } catch (e: InterruptedException) {
      e.printStackTrace()
    }

    val sortMode = prefs.getString(WidgetPreferences.getListSortModeKey(appWidgetId), AppsListPreferences.SORT_BY_NAME) ?: AppsListPreferences.SORT_BY_NAME
    val descending = prefs.getBoolean(WidgetPreferences.getListSortDescendingKey(appWidgetId), false)
    appsList = when (sortMode) {
      "ram" -> {
        if (descending) {
          loadedApps.sortedByDescending { it.ramKb }
        } else {
          loadedApps.sortedBy { it.ramKb }
        }
      }
      "type" -> {
        val typeComparator = if (descending) {
          compareByDescending<AppModel> {
            when {
              it.isProtected -> 4
              it.isPersistentApp -> 3
              it.isSystemApp -> 2
              else -> 1
            }
          }
        } else {
          compareBy<AppModel> {
            when {
              it.isProtected -> 4
              it.isPersistentApp -> 3
              it.isSystemApp -> 2
              else -> 1
            }
          }
        }
        loadedApps.sortedWith(typeComparator.thenBy(java.lang.String.CASE_INSENSITIVE_ORDER) { it.appName })
      }
      else -> {
        if (descending) {
          loadedApps.sortedWith(compareByDescending(java.lang.String.CASE_INSENSITIVE_ORDER) { it.appName })
        } else {
          loadedApps.sortedWith(compareBy(java.lang.String.CASE_INSENSITIVE_ORDER) { it.appName })
        }
      }
    }
  }

  override fun onDestroy() {
    executor?.shutdown()
    thread?.quit()
  }

  override fun getCount(): Int = appsList.size

  override fun getViewAt(position: Int): RemoteViews {
    if (position < 0 || position >= appsList.size) {
      return RemoteViews(context.packageName, R.layout.widget_list_item)
    }
    val app = appsList[position]
    val views = RemoteViews(context.packageName, R.layout.widget_list_item)

    val localCtx = getLocalizedContext(context)
    val formattedRam = formatMemorySize(localCtx, app.ramKb)
    views.setTextViewText(R.id.app_name, app.appName)
    views.setTextViewText(R.id.app_ram, formattedRam)

    val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    val showAppTypeIcons = prefs.getBoolean(WidgetPreferences.getListShowAppTypeIconsKey(appWidgetId), true)
    val appTheme = prefs.getString(KEY_THEME, "dark") ?: "dark"
    val isWhiteTheme = appTheme == "white"

    val primaryTextColor = if (isWhiteTheme) {
      if (app.isProtected) 0x80111111.toInt() else 0xFF111111.toInt()
    } else {
      if (app.isProtected) 0x80FFFFFF.toInt() else 0xFFFFFFFF.toInt()
    }

    val secondaryTextColor = if (isWhiteTheme) {
      if (app.isProtected) 0x50111111.toInt() else 0xA0111111.toInt()
    } else {
      if (app.isProtected) 0x50FFFFFF.toInt() else 0xA0FFFFFF.toInt()
    }

    val iconColor = if (isWhiteTheme) 0xFF111111.toInt() else 0xFFECEFF1.toInt()

    if (app.isProtected) {
      views.setViewVisibility(R.id.btn_close_app, android.view.View.GONE)
      views.setTextColor(R.id.app_name, primaryTextColor)
      views.setTextColor(R.id.app_ram, secondaryTextColor)
      views.setFloat(R.id.app_icon, "setAlpha", 0.4f)
    } else {
      views.setViewVisibility(R.id.btn_close_app, android.view.View.VISIBLE)
      views.setTextColor(R.id.app_name, primaryTextColor)
      views.setTextColor(R.id.app_ram, secondaryTextColor)
      views.setFloat(R.id.app_icon, "setAlpha", 1.0f)
    }

    views.setInt(R.id.btn_close_app, "setColorFilter", iconColor)

    if (showAppTypeIcons) {
      views.setViewVisibility(R.id.app_type_icon, android.view.View.VISIBLE)
      val iconRes = when {
        app.isPersistentApp -> R.drawable.ic_pushpin_outlined
        app.isSystemApp -> R.drawable.ic_settings_outlined
        else -> R.drawable.ic_person_outlined
      }
      views.setImageViewResource(R.id.app_type_icon, iconRes)
      views.setInt(R.id.app_type_icon, "setColorFilter", secondaryTextColor)
    } else {
      views.setViewVisibility(R.id.app_type_icon, android.view.View.GONE)
    }

    try {
      val bitmap = drawableToBitmap(app.appIcon)
      views.setImageViewBitmap(R.id.app_icon, bitmap)
    } catch (e: Exception) {
      e.printStackTrace()
    }

    val fillInIntent = Intent().apply {
      putExtra("package_name", app.packageName)
      putExtra("app_name", app.appName)
      putExtra("app_ram", formattedRam)
    }
    views.setOnClickFillInIntent(R.id.btn_close_app, fillInIntent)

    return views
  }

  override fun getLoadingView(): RemoteViews = RemoteViews(context.packageName, R.layout.widget_loading_item)

  override fun getViewTypeCount(): Int = 1

  override fun getItemId(position: Int): Long = position.toLong()

  override fun hasStableIds(): Boolean = true

  private fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
      if (drawable.bitmap != null) {
        return drawable.bitmap
      }
    }
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
      Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
      Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }
}

private fun getLocalizedContext(context: Context): Context {
  val prefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
  val language = prefs.getString("appLanguage", "system") ?: "system"
  if (language != "system") {
    val locale = java.util.Locale.forLanguageTag(language)
    java.util.Locale.setDefault(locale)
    val config = android.content.res.Configuration(context.resources.configuration)
    config.setLocale(locale)
    return context.createConfigurationContext(config)
  }
  return context
}

private fun formatMemorySize(context: Context, kb: Long): String = when {
  kb < 1024 -> context.getString(R.string.kb_format, kb)
  kb < 1024 * 1024 -> context.getString(R.string.mb_format, kb / 1024f)
  else -> context.getString(R.string.gb_format, kb / (1024f * 1024f))
}
