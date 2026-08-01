package com.yassernull.shappky.core.managers

import android.content.Context
import android.os.Handler
import android.widget.Toast
import com.yassernull.shappky.R

class AppKillHandler(
  private val context: Context,
  private val handler: Handler,
  private val shellManager: ShellManager,
) {
  fun killPackages(
    packageNames: List<String>?,
    onComplete: Runnable?,
    showToast: Boolean = true,
    appendKillAll: Boolean = false,
    getAppRamKb: ((String) -> Long)? = null,
    formatMemorySize: (Long) -> String,
  ) {
    if (!shellManager.hasAnyShellPermission()) {
      shellManager.checkShellPermissions()
      onComplete?.let { handler.post(it) }
      return
    }

    if (packageNames.isNullOrEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }

    val protectedApps = ProtectionManager.getProtectedApps(context)
    val safePackageNames = packageNames.filter { !protectedApps.contains(it) }

    if (safePackageNames.isEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }

    var totalKb = 0L
    for (pkg in safePackageNames) {
      totalKb += getAppRamKb?.invoke(pkg) ?: 0L
    }

    val command = buildSmartKillCommand(safePackageNames, appendKillAll)
    shellManager.runShellCommand(command, onComplete)
    if (showToast) {
      val message = context.getString(R.string.free_up_memory, formatMemorySize(totalKb))
      handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }
  }

  fun killApp(
    packageName: String?,
    onComplete: Runnable?,
    forceKill: Boolean = false,
    appendKillAll: Boolean = false,
    getAppRamKb: ((String) -> Long)? = null,
    formatMemorySize: (Long) -> String,
  ) {
    if (!shellManager.hasAnyShellPermission()) {
      shellManager.checkShellPermissions()
      onComplete?.let { handler.post(it) }
      return
    }
    if (packageName.isNullOrEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }

    if (!forceKill) {
      val protectedApps = ProtectionManager.getProtectedApps(context)
      if (protectedApps.contains(packageName) || ProtectionManager.isAppProtectedByRegex(context, packageName)) {
        onComplete?.let { handler.post(it) }
        return
      }
    }

    val command = buildSmartKillCommand(listOf(packageName), appendKillAll)
    shellManager.runShellCommand(command, onComplete)
    val ramKb = getAppRamKb?.invoke(packageName) ?: 0L
    if (ramKb > 0) {
      val message = context.getString(R.string.free_up_memory, formatMemorySize(ramKb))
      handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }
  }

  companion object {
    fun buildSmartKillCommand(packageNames: List<String>, appendKillAll: Boolean = false): String {
      if (packageNames.isEmpty()) return ""
      val killCommands = packageNames.joinToString("; ") { "am kill " + it }
      val forceStopCommands = packageNames.joinToString("; ") { "if pidof " + it + " > /dev/null; then am force-stop " + it + "; fi" }
      val kill9Commands = packageNames.joinToString("; ") {
        val escapedPkg = it.replace(".", "\\.")
        "pids=${'$'}(${ShellManager.TOYBOX_PATH} ps -A -o pid,name | grep -E '" + escapedPkg + "([^A-Za-z]|\$)' | grep -v '[-@]' | awk '{print ${'$'}1}'); if [ ! -z \"${'$'}pids\" ]; then kill -9 ${'$'}pids 2>/dev/null; fi"
      }
      val baseCommand = killCommands + "; sleep 0.2; " + forceStopCommands + "; sleep 0.2; " + kill9Commands
      return if (appendKillAll) baseCommand + "; am kill-all" else baseCommand
    }
  }
}
