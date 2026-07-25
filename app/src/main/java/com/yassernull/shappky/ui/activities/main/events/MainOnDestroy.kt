package com.yassernull.shappky.ui.activities.main.events

import com.yassernull.shappky.ui.activities.main.MainActivity
import com.yassernull.shappky.ui.activities.main.logic.AppsListLogic

fun MainActivity.handleOnDestroy() {
  AppsListLogic.shellManager.removeShizukuPermissionListener()
  AppsListLogic.autoRefreshManager.stopAll()
  AppsListLogic.ramMonitor.stopMonitoring()
}
