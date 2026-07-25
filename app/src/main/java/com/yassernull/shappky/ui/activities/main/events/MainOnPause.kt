package com.yassernull.shappky.ui.activities.main.events

import com.yassernull.shappky.services.ShappkyService
import com.yassernull.shappky.ui.activities.main.MainActivity
import com.yassernull.shappky.ui.activities.main.logic.AppsListLogic

fun MainActivity.handleOnPause() {
  ShappkyService.unregisterListener(serviceStateListener)
  AppsListLogic.ramMonitor.stopMonitoring()
  AppsListLogic.autoRefreshManager.stopAll()
}
