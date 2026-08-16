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
    val safePackageNames = packageNames.filter { !ProtectionManager.isPackageProtected(context, it) }

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
    KillTracker.markKilledAll(safePackageNames)
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
      if (ProtectionManager.isPackageProtected(context, packageName)) {
        onComplete?.let { handler.post(it) }
        return
      }
    }

    val command = buildSmartKillCommand(listOf(packageName), appendKillAll)
    shellManager.runShellCommand(command, onComplete)
    KillTracker.markKilled(packageName)
    val ramKb = getAppRamKb?.invoke(packageName) ?: 0L
    if (ramKb > 0) {
      val message = context.getString(R.string.free_up_memory, formatMemorySize(ramKb))
      handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }
  }

  companion object {
    fun buildSmartKillCommand(packageNames: List<String>, appendKillAll: Boolean = false): String {
      if (packageNames.isEmpty()) return ""
      val perPackage = packageNames.joinToString("; ") { pkg ->
        val escapedPkg = pkg.replace(".", "\\.")
        val truncatedPkg = pkg.take(15)
        val escapedTruncatedPkg = truncatedPkg.replace(".", "\\.")
        "am kill " + pkg +
          "; sleep 0.2; if " + ShellManager.TOYBOX_PATH + " pidof " + pkg + " > /dev/null 2>&1 || " + ShellManager.TOYBOX_PATH + " pidof " + truncatedPkg + " > /dev/null 2>&1; then am force-stop " + pkg + "; sleep 0.2; fi" +
          "; pids=${'$'}(" + ShellManager.TOYBOX_PATH + " ps -A -o pid,name | grep -oE '[0-9]+ (" + escapedPkg + "|" + escapedTruncatedPkg + ")([^A-Za-z0-9]|\$)' | awk '{print ${'$'}1}'); if [ ! -z \"${'$'}pids\" ]; then kill -9 ${'$'}pids 2>/dev/null; fi"
      }
      return if (appendKillAll) perPackage + "; am kill-all" else perPackage
    }
  }
}
