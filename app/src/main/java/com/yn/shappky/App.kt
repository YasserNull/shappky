package com.yn.shappky

import android.app.Activity
import android.app.Application
import android.os.Bundle

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    nativeLibraryDir = applicationInfo.nativeLibraryDir
    registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
      private var startedActivities = 0

      override fun onActivityStarted(activity: Activity) {
        startedActivities++
        isAppInForeground = true
      }

      override fun onActivityStopped(activity: Activity) {
        startedActivities--
        if (startedActivities <= 0) {
          isAppInForeground = false
        }
      }

      override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
      override fun onActivityResumed(activity: Activity) {}
      override fun onActivityPaused(activity: Activity) {}
      override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
      override fun onActivityDestroyed(activity: Activity) {}
    })
  }

  companion object {
    @Volatile
    var isAppInForeground = false

    var nativeLibraryDir: String = ""
      private set
  }
}
