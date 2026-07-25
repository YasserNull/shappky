package com.yassernull.shappky.core.managers

import android.os.Handler

class AutoRefreshManager(private val handler: Handler) {
  private var appsAutoRefreshRunnable: Runnable? = null
  private var appsRamUsageRunnable: Runnable? = null

  fun updateAppsAutoRefresh(
    enabled: Boolean,
    intervalMs: Long,
    onRefresh: () -> Unit,
  ) {
    if (enabled) {
      startAppsAutoRefresh(intervalMs, onRefresh)
    } else {
      stopAppsAutoRefresh()
    }
  }

  private fun startAppsAutoRefresh(intervalMs: Long, onRefresh: () -> Unit) {
    if (appsAutoRefreshRunnable != null) return
    appsAutoRefreshRunnable = object : Runnable {
      override fun run() {
        onRefresh()
        handler.postDelayed(this, intervalMs)
      }
    }
    handler.postDelayed(requireNotNull(appsAutoRefreshRunnable), intervalMs)
  }

  fun stopAppsAutoRefresh() {
    appsAutoRefreshRunnable?.let { handler.removeCallbacks(it) }
    appsAutoRefreshRunnable = null
  }

  fun updateAppsRamUsageAutoRefresh(
    enabled: Boolean,
    intervalMs: Long,
    onRefresh: () -> Unit,
  ) {
    if (enabled) {
      startAppsRamUsageAutoRefresh(intervalMs, onRefresh)
    } else {
      stopAppsRamUsageAutoRefresh()
    }
  }

  private fun startAppsRamUsageAutoRefresh(intervalMs: Long, onRefresh: () -> Unit) {
    if (appsRamUsageRunnable != null) return
    appsRamUsageRunnable = object : Runnable {
      override fun run() {
        onRefresh()
        handler.postDelayed(this, intervalMs)
      }
    }
    handler.postDelayed(requireNotNull(appsRamUsageRunnable), intervalMs)
  }

  fun stopAppsRamUsageAutoRefresh() {
    appsRamUsageRunnable?.let { handler.removeCallbacks(it) }
    appsRamUsageRunnable = null
  }

  fun stopAll() {
    stopAppsAutoRefresh()
    stopAppsRamUsageAutoRefresh()
  }
}
