package com.yn.shappky.ui.activities.main

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.yn.shappky.ui.activities.main.events.handleOnCreate
import com.yn.shappky.ui.activities.main.events.handleOnDestroy
import com.yn.shappky.ui.activities.main.events.handleOnPause
import com.yn.shappky.ui.activities.main.events.handleOnResume

class MainActivity : ComponentActivity() {

  internal val serviceStateListener = { running: Boolean ->
    com.yn.shappky.ui.activities.main.logic.AppsListLogic.isServiceRunning = running
  }

  @Deprecated("Deprecated in Java")
  @Suppress("DEPRECATION")
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    PermissionHandler.handlePermissionsResult(this, requestCode, com.yn.shappky.ui.activities.main.logic.AppsListLogic.shellManager)
  }

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(com.yn.shappky.utils.LanguageHelper.getLanguageContext(newBase))
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
