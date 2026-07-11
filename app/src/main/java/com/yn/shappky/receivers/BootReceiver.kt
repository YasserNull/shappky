package com.yn.shappky.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.yn.shappky.utils.TriggerServiceManager

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Log.d("BootReceiver", "onReceive: action=${intent.action}")
    if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
      intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
      intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
    ) {
      TriggerServiceManager.updateTriggerServiceState(context)
    }
  }
}
