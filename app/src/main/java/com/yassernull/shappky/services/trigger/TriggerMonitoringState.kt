package com.yassernull.shappky.services.trigger

class TriggerMonitoringState {
  @Volatile
  var lastSharedForeground: String? = null

  @Volatile
  var pendingSleepEvent = false

  @Volatile
  var pendingWakeEvent = false
}
