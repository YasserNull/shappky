package com.yn.shappky.model

import android.graphics.drawable.Drawable

data class AppModel(
    var appName: String,
    var packageName: String,
    var appRam: String,
    var ramKb: Long,
    var appIcon: Drawable,
    var isSystemApp: Boolean,
    var isPersistentApp: Boolean,
    var isProtected: Boolean,
    var isSelected: Boolean = false,
)
