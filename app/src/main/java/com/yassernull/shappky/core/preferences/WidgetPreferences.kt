package com.yassernull.shappky.core.preferences

object WidgetPreferences {
  fun getBgColorKey(appWidgetId: Int) = "widget_bg_color_$appWidgetId"
  fun getBgSizeKey(appWidgetId: Int) = "widget_bg_size_$appWidgetId"
  fun getIconColorKey(appWidgetId: Int) = "widget_icon_color_$appWidgetId"
  fun getIconSizeKey(appWidgetId: Int) = "widget_icon_size_$appWidgetId"
  fun getTriggerIdKey(appWidgetId: Int) = "widget_trigger_id_$appWidgetId"
  fun getShowLabelKey(appWidgetId: Int) = "widget_show_label_$appWidgetId"

  fun getListBgColorKey(appWidgetId: Int) = "widget_list_bg_color_$appWidgetId"
  fun getListAutoBgKey(appWidgetId: Int) = "widget_list_auto_bg_$appWidgetId"

  fun getListShowUserAppsKey(appWidgetId: Int) = "widget_list_show_user_apps_$appWidgetId"
  fun getListShowSystemAppsKey(appWidgetId: Int) = "widget_list_show_system_apps_$appWidgetId"
  fun getListShowPersistentAppsKey(appWidgetId: Int) = "widget_list_show_persistent_apps_$appWidgetId"
  fun getListShowProtectedAppsKey(appWidgetId: Int) = "widget_list_show_protected_apps_$appWidgetId"
  fun getListShowAppTypeIconsKey(appWidgetId: Int) = "widget_list_show_app_type_icons_$appWidgetId"

  fun getListSortModeKey(appWidgetId: Int) = "widget_list_sort_mode_$appWidgetId"
  fun getListSortDescendingKey(appWidgetId: Int) = "widget_list_sort_descending_$appWidgetId"

  fun getListAutoRefreshAppsKey(appWidgetId: Int) = "widget_list_auto_refresh_apps_$appWidgetId"
  fun getListRamBarRefreshKey(appWidgetId: Int) = "widget_list_ram_bar_refresh_$appWidgetId"
}
