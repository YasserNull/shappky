package com.yn.shappky

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat

class ShappkyQuickTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_shappky)
        tile.label = getString(R.string.shappky_service)
        tile.state = if (ShappkyService.isRunning()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (tile.state == Tile.STATE_INACTIVE) {
            startForegroundService(Intent(this, ShappkyService::class.java))
            tile.state = Tile.STATE_ACTIVE
            tile.label = getString(R.string.shappky_service_running)
        } else {
            stopService(Intent(this, ShappkyService::class.java))
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.shappky_service)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_shappky)
        tile.updateTile()
    }
}
