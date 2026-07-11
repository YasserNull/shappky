package com.yn.shappky.ui.activities.main.events

import com.yn.shappky.services.ShappkyService
import com.yn.shappky.ui.activities.main.MainActivity
import com.yn.shappky.ui.activities.main.logic.AppsListLogic

fun MainActivity.handleOnPause() {
  ShappkyService.unregisterListener(serviceStateListener)
  AppsListLogic.ramMonitor.stopMonitoring()
  AppsListLogic.autoRefreshManager.stopAll()
}
