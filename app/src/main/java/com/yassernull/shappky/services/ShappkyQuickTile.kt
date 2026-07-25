package com.yassernull.shappky.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.yassernull.shappky.R
import com.yassernull.shappky.services.ShappkyService

class ShappkyQuickTile : TileService() {
  override fun onStartListening() {
    super.onStartListening()
    val tile = qsTile ?: return
    val isRunning = ShappkyService.isRunning()
    tile.icon = Icon.createWithResource(this, R.drawable.ic_shappky)
    tile.label = getString(R.string.shappky_service)
    tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
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
      val handler = android.os.Handler(android.os.Looper.getMainLooper())
      val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
      val shellManager = com.yassernull.shappky.core.managers.ShellManager(this, handler, executor)
      shellManager.checkShellPermissions()
      val hasPermission = shellManager.hasAnyShellPermission()
      executor.shutdown()
      if (!hasPermission) {
        android.widget.Toast.makeText(this, getString(R.string.shell_permission_required), android.widget.Toast.LENGTH_LONG).show()
        return
      }
      startForegroundService(Intent(this, ShappkyService::class.java))
      tile.state = Tile.STATE_ACTIVE
      tile.label = getString(R.string.shappky_service)
    } else {
      stopService(Intent(this, ShappkyService::class.java))
      tile.state = Tile.STATE_INACTIVE
      tile.label = getString(R.string.shappky_service)
    }
    tile.icon = Icon.createWithResource(this, R.drawable.ic_shappky)
    tile.updateTile()
  }
}
