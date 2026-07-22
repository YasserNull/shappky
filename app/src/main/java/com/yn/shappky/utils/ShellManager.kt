package com.yn.shappky.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku
import java.util.concurrent.ExecutorService
import java.util.function.Consumer

class ShellManager(
  private val context: Context,
  private val handler: Handler,
  private val executor: ExecutorService,
) {
  private var hasRoot: Boolean? = null
  private var shizukuPermissionListener: Shizuku.OnRequestPermissionResultListener? = null
  private var onShizukuServiceConnected: Runnable? = null

  private val shizukuBinderReceivedListener = Shizuku.OnBinderReceivedListener {
    Log.d(TAG, "Shizuku binder received callback started")
    val mode = getPermissionMode()
    val permission = hasShizukuPermission()
    Log.d(TAG, "Shizuku binder received state mode=$mode, hasPermission=$permission")
    if (mode == "shizuku" && permission) {
      Log.d(TAG, "Shizuku binder ready, notifying listeners")
      onShizukuServiceConnected?.let { handler.post(it) }
    }
  }

  private val shizukuBinderDeadListener = Shizuku.OnBinderDeadListener {
    Log.w(TAG, "Shizuku binder died")
  }

  init {
    Shizuku.addBinderReceivedListenerSticky(shizukuBinderReceivedListener)
    Shizuku.addBinderDeadListener(shizukuBinderDeadListener)
  }

  private fun getPermissionMode(): String = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    .getString("permissionMode", "shizuku") ?: "shizuku"

  fun setShizukuPermissionListener(listener: Shizuku.OnRequestPermissionResultListener?) {
    shizukuPermissionListener = listener
    if (listener != null) {
      Shizuku.addRequestPermissionResultListener(listener)
    }
  }

  fun removeShizukuPermissionListener() {
    shizukuPermissionListener?.let {
      Shizuku.removeRequestPermissionResultListener(it)
    }
    Shizuku.removeBinderReceivedListener(shizukuBinderReceivedListener)
    Shizuku.removeBinderDeadListener(shizukuBinderDeadListener)
  }

  fun setOnShizukuServiceConnected(listener: Runnable?) {
    onShizukuServiceConnected = listener
  }

  fun hasRootAccess(): Boolean {
    if (hasRoot == null) {
      hasRoot = try {
        Shell.getShell().isRoot
      } catch (e: Exception) {
        false
      }
    }
    return hasRoot == true
  }

  fun hasShizukuPermission(): Boolean {
    val ping = Shizuku.pingBinder()
    val permission = if (ping) Shizuku.checkSelfPermission() else PackageManager.PERMISSION_DENIED
    return ping && permission == PackageManager.PERMISSION_GRANTED
  }

  private fun safeGetShizukuVersion(): Int = try {
    Shizuku.getVersion()
  } catch (e: RuntimeException) {
    -1
  }

  fun checkShellPermissions() {
    val mode = getPermissionMode()
    val ping = Shizuku.pingBinder()
    Log.d(TAG, "checkShellPermissions started mode=$mode, ping=$ping")
    if (ping) {
      val isPreV11 = Shizuku.isPreV11()
      val permission = Shizuku.checkSelfPermission()
      if (permission != PackageManager.PERMISSION_GRANTED) {
        if (mode == "shizuku") {
          Shizuku.requestPermission(0)
        }
      } else if (mode == "shizuku") {
        onShizukuServiceConnected?.let { handler.post(it) }
      }
    }
  }

  fun hasAnyShellPermission(): Boolean {
    val mode = getPermissionMode()
    if (mode == "shizuku") {
      return hasShizukuPermission()
    }
    return hasRootAccess()
  }

  fun isShellCommandReady(): Boolean {
    val mode = getPermissionMode()
    if (mode == "root") {
      return hasRootAccess()
    }
    return hasShizukuPermission()
  }

  fun runShellCommand(command: String, onSuccess: Runnable?) {
    if (!executor.isShutdown) {
      executor.execute {
        val mode = getPermissionMode()
        var executed = false
        if (mode == "root" && hasRootAccess()) {
          if (executeRootCommand(command, onSuccess, null)) executed = true
        }
        if (!executed && mode == "shizuku" && hasShizukuPermission()) {
          if (executeShizukuCommand(command, onSuccess)) executed = true
        }
        if (!executed) {
          onSuccess?.let { handler.post(it) }
        }
      }
    }
  }

  fun runShellCommandWithOutput(command: String, outputProcessor: Consumer<String>) {
    if (!executor.isShutdown) {
      executor.execute {
        var executed = false
        if (getPermissionMode() == "root" && hasRootAccess()) {
          if (executeRootCommand(command, null, outputProcessor)) executed = true
        }
        if (!executed && getPermissionMode() == "shizuku" && hasShizukuPermission()) {
          executeShizukuCommandWithOutput(command, outputProcessor)
        }
      }
    }
  }

  fun runShellCommandAndGetFullOutput(command: String): String? {
    val mode = getPermissionMode()
    return when {
      mode == "root" && hasRootAccess() -> {
        executeRootCommandAndGetFullOutput(command)
      }
      mode == "shizuku" && hasShizukuPermission() -> {
        executeShizukuCommandAndGetFullOutput(command)
      }
      else -> null
    }
  }

  private fun executeRootCommand(
    command: String,
    onSuccess: Runnable?,
    outputProcessor: Consumer<String>?,
  ): Boolean = try {
    val result = Shell.cmd(command).exec()
    if (outputProcessor != null) {
      result.out.forEach { line -> handler.post { outputProcessor.accept(line) } }
      result.err.forEach { line -> handler.post { outputProcessor.accept("ERROR: $line") } }
    }
    onSuccess?.let { handler.post(it) }
    result.isSuccess
  } catch (e: Exception) {
    e.printStackTrace()
    false
  }

  private fun executeShizukuCommand(command: String, onSuccess: Runnable?): Boolean = try {
    Log.d(TAG, "Shizuku command executing command=$command")
    val process = createShizukuProcess(command)
    process.waitFor()
    Log.d(TAG, "Shizuku command executed command=$command")
    onSuccess?.let { handler.post(it) }
    true
  } catch (e: Exception) {
    Log.e(TAG, "Shizuku command Exception command=$command", e)
    e.printStackTrace()
    false
  }

  private fun executeShizukuCommandWithOutput(
    command: String,
    outputProcessor: Consumer<String>,
  ): Boolean = try {
    Log.d(TAG, "Shizuku command with output executing command=$command")
    val process = createShizukuProcess(command)

    val stdoutThread = Thread {
      process.inputStream.bufferedReader().useLines { lines ->
        lines.forEach { line -> handler.post { outputProcessor.accept(line) } }
      }
    }

    val stderrThread = Thread {
      process.errorStream.bufferedReader().useLines { lines ->
        lines.forEach { line -> handler.post { outputProcessor.accept("ERROR: $line") } }
      }
    }

    stdoutThread.start()
    stderrThread.start()
    process.waitFor()
    stdoutThread.join()
    stderrThread.join()
    true
  } catch (e: Exception) {
    Log.e(TAG, "Shizuku command with output failed command=$command", e)
    e.printStackTrace()
    false
  }

  private fun executeRootCommandAndGetFullOutput(command: String): String? {
    val output = StringBuilder()
    return try {
      val result = Shell.cmd(command).exec()
      result.out.forEach { line -> output.append(line).append("\n") }
      result.err.forEach { line -> output.append("ERROR: ").append(line).append("\n") }
      output.toString()
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  private fun executeShizukuCommandAndGetFullOutput(command: String): String? = try {
    Log.d(TAG, "Shizuku full output command executing command=$command")
    val process = createShizukuProcess(command)

    val output = StringBuilder()
    val stdoutThread = Thread {
      process.inputStream.bufferedReader().useLines { lines ->
        lines.forEach { line -> synchronized(output) { output.append(line).append("\n") } }
      }
    }

    val stderrThread = Thread {
      process.errorStream.bufferedReader().useLines { lines ->
        lines.forEach { line -> synchronized(output) { output.append("ERROR: ").append(line).append("\n") } }
      }
    }

    stdoutThread.start()
    stderrThread.start()
    process.waitFor()
    stdoutThread.join()
    stderrThread.join()

    output.toString()
  } catch (e: Exception) {
    Log.e(TAG, "Shizuku command failed command=$command", e)
    e.printStackTrace()
    null
  }

  private fun createShizukuProcess(command: String): java.lang.Process {
    val method = Shizuku::class.java.getDeclaredMethod(
      "newProcess",
      Array<String>::class.java,
      Array<String>::class.java,
      String::class.java,
    )
    method.isAccessible = true
    return method.invoke(null, arrayOf("sh", "-c", command), null, null) as java.lang.Process
  }

  fun deployToybox(nativeLibraryDir: String) {
    if (!executor.isShutdown) {
      executor.execute {
        val mode = getPermissionMode()
        val source = "$nativeLibraryDir/libtoybox.so"
        val dest = TOYBOX_PATH
        val checkCmd = "[ -f $dest ] && [ -x $dest ]"
        val copyCmd = "cp $source $dest && chmod 755 $dest"
        val cmd = "if $checkCmd; then exit 0; else $copyCmd; fi"
        try {
          when {
            mode == "root" && hasRootAccess() -> Shell.cmd(cmd).exec()
            mode == "shizuku" && hasShizukuPermission() -> {
              val process = createShizukuProcess(cmd)
              process.waitFor()
            }
          }
          Log.d(TAG, "Toybox deployed to $dest")
        } catch (e: Exception) {
          Log.e(TAG, "Failed to deploy toybox", e)
        }
      }
    }
  }

  companion object {
    const val TOYBOX_PATH = "/data/local/tmp/toybox"
    private const val TAG = "ShappkyShell"
  }
}
