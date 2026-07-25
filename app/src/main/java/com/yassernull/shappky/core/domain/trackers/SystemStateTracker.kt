package com.yassernull.shappky.core.domain.trackers

import android.app.NotificationManager
import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log

class SystemStateTracker(private val context: Context) {

  var lastWifiState: Boolean? = null
  var lastBluetoothState: Boolean? = null
  var lastMobileDataState: Boolean? = null
  var lastAirplaneModeState: Boolean? = null
  var lastGpsState: Boolean? = null
  var lastHotspotState: Boolean? = null
  var lastDndState: Boolean? = null
  var lastNfcState: Boolean? = null
  var lastInteractiveState: Boolean? = null

  var currentWifi: Boolean? = null
  var currentBluetooth: Boolean? = null
  var currentMobileData: Boolean? = null
  var currentAirplaneMode: Boolean? = null
  var currentGps: Boolean? = null
  var currentHotspot: Boolean? = null
  var currentDnd: Boolean? = null
  var currentNfc: Boolean? = null
  var currentInteractive: Boolean = true

  fun initializeStates() {
    try {
      updateCurrentStates()
      saveCurrentStatesAsLast()
    } catch (e: Exception) {
      Log.e("SystemStateTracker", "Error initializing service states", e)
    }
  }

  fun updateCurrentStates() {
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    currentWifi = try {
      wifiManager?.isWifiEnabled
    } catch (_: Exception) {
      null
    }

    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    val bluetoothAdapter = bluetoothManager?.adapter
    currentBluetooth = try {
      bluetoothAdapter?.isEnabled
    } catch (_: Exception) {
      null
    }

    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    currentMobileData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      try {
        telephonyManager?.isDataEnabled
      } catch (_: Exception) {
        null
      }
    } else {
      try {
        val method = telephonyManager?.javaClass?.getDeclaredMethod("getDataEnabled")
        method?.invoke(telephonyManager) as? Boolean
      } catch (_: Exception) {
        null
      }
    }

    currentAirplaneMode = try {
      Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    } catch (_: Exception) {
      false
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    currentGps = try {
      locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)
    } catch (_: Exception) {
      null
    }

    currentHotspot = if (wifiManager != null) {
      try {
        val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
        method.isAccessible = true
        method.invoke(wifiManager) as Boolean
      } catch (_: Exception) {
        false
      }
    } else {
      false
    }

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    currentDnd = try {
      if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
      } else {
        false
      }
    } catch (_: Exception) {
      false
    }

    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    currentNfc = try {
      nfcAdapter?.isEnabled
    } catch (_: Exception) {
      null
    }

    val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
    currentInteractive = try {
      pm.isInteractive
    } catch (_: Exception) {
      true
    }
  }

  fun saveCurrentStatesAsLast() {
    lastWifiState = currentWifi
    lastBluetoothState = currentBluetooth
    lastMobileDataState = currentMobileData
    lastAirplaneModeState = currentAirplaneMode
    lastGpsState = currentGps
    lastHotspotState = currentHotspot
    lastDndState = currentDnd
    lastNfcState = currentNfc
    lastInteractiveState = currentInteractive
  }

  fun hasServiceStateChanged(serviceKey: String): Boolean = when (serviceKey) {
    "wifi" -> lastWifiState != null && lastWifiState != currentWifi
    "bluetooth" -> lastBluetoothState != null && lastBluetoothState != currentBluetooth
    "mobile_data" -> lastMobileDataState != null && lastMobileDataState != currentMobileData
    "airplane_mode" -> lastAirplaneModeState != null && lastAirplaneModeState != currentAirplaneMode
    "gps" -> lastGpsState != null && lastGpsState != currentGps
    "hotspot" -> lastHotspotState != null && lastHotspotState != currentHotspot
    "dnd" -> lastDndState != null && lastDndState != currentDnd
    "nfc" -> lastNfcState != null && lastNfcState != currentNfc
    else -> false
  }
}
