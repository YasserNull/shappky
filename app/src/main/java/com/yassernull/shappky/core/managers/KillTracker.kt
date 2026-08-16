package com.yassernull.shappky.core.managers

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

object KillTracker {
  private const val TAG = "KillTracker"
  private val recentShappkyKills = ConcurrentHashMap.newKeySet<String>()

  fun markKilled(pkg: String) {
    recentShappkyKills.add(pkg)
    Log.d(TAG, "Kill recorded: $pkg")
  }

  fun markKilledAll(packages: Collection<String>) {
    packages.forEach { pkg ->
      recentShappkyKills.add(pkg)
      Log.d(TAG, "Kill recorded: $pkg")
    }
  }

  fun contains(pkg: String): Boolean = recentShappkyKills.contains(pkg)

  fun getKilledPackages(): Set<String> = recentShappkyKills.toSet()

  fun cleanUp(runningPackages: Set<String>) {
    val iterator = recentShappkyKills.iterator()
    while (iterator.hasNext()) {
      val pkg = iterator.next()
      if (!runningPackages.contains(pkg)) {
        iterator.remove()
        Log.d(TAG, "Kill record cleaned: $pkg")
      }
    }
  }
}
