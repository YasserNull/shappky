package com.yassernull.shappky.ui.activities.main

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.yassernull.shappky.ui.activities.main.events.handleOnCreate
import com.yassernull.shappky.ui.activities.main.events.handleOnDestroy
import com.yassernull.shappky.ui.activities.main.events.handleOnPause
import com.yassernull.shappky.ui.activities.main.events.handleOnResume

class MainActivity : ComponentActivity() {

  internal val serviceStateListener = { running: Boolean ->
    com.yassernull.shappky.ui.activities.main.logic.AppsListLogic.isServiceRunning = running
  }

  @Deprecated("Deprecated in Java")
  @Suppress("DEPRECATION")
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    PermissionHandler.handlePermissionsResult(this, requestCode, com.yassernull.shappky.ui.activities.main.logic.AppsListLogic.shellManager)
  }

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(com.yassernull.shappky.core.managers.LocaleManager.getLanguageContext(newBase))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleOnCreate(savedInstanceState)
  }

  override fun onDestroy() {
    super.onDestroy()
    handleOnDestroy()
  }

  override fun onResume() {
    super.onResume()
    handleOnResume()
  }

  override fun onPause() {
    super.onPause()
    handleOnPause()
  }
}
