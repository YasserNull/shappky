package com.yassernull.shappky.core.managers

import android.content.Context
import android.os.Handler
import com.yassernull.shappky.data.models.AppModel
import java.util.concurrent.ExecutorService
import java.util.function.Consumer

class BackgroundAppManager(
  context: Context,
  handler: Handler,
  executor: ExecutorService,
  shellManager: ShellManager,
) {
  private val loader = AppProcessLoader(context, handler, executor, shellManager)
  private val killHandler = AppKillHandler(context, handler, shellManager)

  fun formatMemorySize(kb: Long): String = loader.formatMemorySize(kb)

  fun loadBackgroundApps(callback: Consumer<List<AppModel>>?) {
    loader.loadBackgroundApps(callback)
  }

  fun loadAllApps(callback: Consumer<List<AppModel>>) {
    loader.loadAllApps(callback)
  }

  fun loadAppsRamUsage(packageNames: List<String>, callback: Consumer<Map<String, Long>>) {
    loader.loadAppsRamUsage(packageNames, callback)
  }

  fun loadAppDetailedInfo(app: AppModel, callback: Consumer<com.yassernull.shappky.data.models.AppDetailedInfo>) {
    loader.loadAppDetailedInfo(app, callback)
  }

  fun getHiddenApps(): Set<String> = loader.getHiddenApps()

  fun saveHiddenApps(hiddenApps: Set<String>) {
    loader.saveHiddenApps(hiddenApps)
  }

  fun killPackages(packageNames: List<String>?, onComplete: Runnable?, showToast: Boolean = true, appendKillAll: Boolean = false) {
    killHandler.killPackages(
      packageNames = packageNames,
      onComplete = onComplete,
      showToast = showToast,
      appendKillAll = appendKillAll,
      getAppRamKb = { pkg -> loader.currentAppsList.firstOrNull { it.packageName == pkg }?.ramKb ?: 0L },
      formatMemorySize = loader::formatMemorySize,
    )
  }

  fun killApp(packageName: String?, onComplete: Runnable?, forceKill: Boolean = false, appendKillAll: Boolean = false) {
    killHandler.killApp(
      packageName = packageName,
      onComplete = onComplete,
      forceKill = forceKill,
      appendKillAll = appendKillAll,
      getAppRamKb = { pkg -> loader.currentAppsList.firstOrNull { it.packageName == pkg }?.ramKb ?: 0L },
      formatMemorySize = loader::formatMemorySize,
    )
  }

  fun setShowUserApps(show: Boolean) {
    loader.showUserApps = show
  }

  fun setShowSystemApps(show: Boolean) {
    loader.showSystemApps = show
  }

  fun setShowPersistentApps(show: Boolean) {
    loader.showPersistentApps = show
  }

  fun setShowProtectedApps(show: Boolean) {
    loader.showProtectedApps = show
  }

  fun getAppsList(): List<AppModel> = loader.getAppsList()

  companion object {
    fun buildSmartKillCommand(packageNames: List<String>, appendKillAll: Boolean = false): String = AppKillHandler.buildSmartKillCommand(packageNames, appendKillAll)
  }
}
