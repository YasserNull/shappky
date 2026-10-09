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
    getAppRamKb: ((String) -> Long)? = null, // kept for signature compatibility
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

    val safePackageNames = packageNames.filter { !ProtectionManager.isPackageProtected(context, it) }

    if (safePackageNames.isEmpty()) {
      onComplete?.let { handler.post(it) }
      return
    }

    val command = buildSmartKillCommand(safePackageNames, appendKillAll)
    Thread {
      val output = shellManager.runShellCommandAndGetFullOutput(command)
      KillTracker.markKilledAll(safePackageNames)

      android.util.Log.d("AppKillHandler", "Kill output for packages $safePackageNames:\n$output")

      var totalFreedKb = 0L
      if (output != null) {
        val regex = Regex("FREED:[^:]+:([0-9]+)")
        regex.findAll(output).forEach { match ->
          val freed = match.groupValues[1].toLong()
          android.util.Log.d("AppKillHandler", "Parsed FREED: $freed KB")
          totalFreedKb += freed
        }
      }

      android.util.Log.d("AppKillHandler", "Total freed KB: $totalFreedKb")

      if (showToast) {
        if (totalFreedKb > 0) {
          val message = context.getString(R.string.free_up_memory, formatMemorySize(totalFreedKb))
          handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
        } else {
          handler.post { Toast.makeText(context, context.getString(R.string.failed_to_stop_apps), Toast.LENGTH_LONG).show() }
        }
      }
      onComplete?.let { handler.post(it) }
    }.start()
  }

  fun killApp(
    packageName: String?,
    onComplete: Runnable?,
    forceKill: Boolean = false,
    appendKillAll: Boolean = false,
    getAppRamKb: ((String) -> Long)? = null, // kept for signature compatibility
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
    Thread {
      val output = shellManager.runShellCommandAndGetFullOutput(command)
      KillTracker.markKilled(packageName)

      android.util.Log.d("AppKillHandler", "Kill output for package $packageName:\n$output")

      var totalFreedKb = 0L
      if (output != null) {
        val regex = Regex("FREED:[^:]+:([0-9]+)")
        regex.findAll(output).forEach { match ->
          val freed = match.groupValues[1].toLong()
          android.util.Log.d("AppKillHandler", "Parsed FREED: $freed KB")
          totalFreedKb += freed
        }
      }

      android.util.Log.d("AppKillHandler", "Total freed KB: $totalFreedKb")

      if (totalFreedKb > 0) {
        val message = context.getString(R.string.free_up_memory, formatMemorySize(totalFreedKb))
        handler.post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
      } else {
        handler.post { Toast.makeText(context, context.getString(R.string.failed_to_stop_apps), Toast.LENGTH_LONG).show() }
      }
      onComplete?.let { handler.post(it) }
    }.start()
  }

  companion object {
    fun buildSmartKillCommand(packageNames: List<String>, appendKillAll: Boolean = false): String {
      if (packageNames.isEmpty()) return ""
      val perPackage = packageNames.joinToString("; ") { pkg ->
        val escapedPkg = pkg.replace(".", "\\.")
        val truncatedPkg = pkg.take(15)
        val escapedTruncatedPkg = truncatedPkg.replace(".", "\\.")
        val p = "^(" + escapedPkg + "|" + escapedTruncatedPkg + ")([^A-Za-z0-9]|$)"
        val grepRegex = "[0-9]+ (" + escapedPkg + "|" + escapedTruncatedPkg + ")([^A-Za-z0-9]|\$)"

        "before=${'$'}(" + ShellManager.TOYBOX_PATH + " ps -A -o rss,name | " + ShellManager.TOYBOX_PATH + " grep -oE '" + grepRegex + "' | (sum=0; while read -r rss rest; do sum=${'$'}((${'$'}sum + ${'$'}rss)); done; echo ${'$'}sum)); " +
          "am kill " + pkg + "; " +
          "if " + ShellManager.TOYBOX_PATH + " pidof " + pkg + " > /dev/null 2>&1 || " + ShellManager.TOYBOX_PATH + " pidof " + truncatedPkg + " > /dev/null 2>&1; then am force-stop " + pkg + "; fi; " +
          "pids=${'$'}(" + ShellManager.TOYBOX_PATH + " ps -A -o pid,name | " + ShellManager.TOYBOX_PATH + " grep -oE '" + grepRegex + "' | (while read -r pid rest; do printf \"%s \" \"${'$'}pid\"; done)); " +
          "if [ ! -z \"${'$'}pids\" ] && [ \"${'$'}pids\" != \" \" ]; then kill -9 ${'$'}pids 2>/dev/null; fi; " +
          "after=${'$'}(" + ShellManager.TOYBOX_PATH + " ps -A -o rss,name | " + ShellManager.TOYBOX_PATH + " grep -oE '" + grepRegex + "' | (sum=0; while read -r rss rest; do sum=${'$'}((${'$'}sum + ${'$'}rss)); done; echo ${'$'}sum)); " +
          "freed=${'$'}((${'$'}before - ${'$'}after)); " +
          "if [ \"${'$'}freed\" -lt 0 ]; then freed=0; fi; " +
          "echo \"FREED:$pkg:${'$'}freed\""
      }
      return if (appendKillAll) perPackage + "; am kill-all" else perPackage
    }
  }
}
