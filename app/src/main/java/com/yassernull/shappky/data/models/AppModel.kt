package com.yassernull.shappky.data.models

import android.graphics.drawable.Drawable

data class AppModel(
  var appName: String,
  var packageName: String,
  var appRam: String,
  var ramKb: Long,
  var appCpu: String = "",
  var cpuPercent: Double = 0.0,
  var appIcon: Drawable,
  var isSystemApp: Boolean,
  var isPersistentApp: Boolean,
  var isProtected: Boolean,
  var isSelected: Boolean = false,
)
