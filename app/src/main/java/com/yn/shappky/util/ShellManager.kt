package com.yn.shappky.util

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.yn.shappky.IShizukuShellService
import com.yn.shappky.shizuku.ShizukuShellService
import java.util.concurrent.ExecutorService
import java.util.function.Consumer
import rikka.shizuku.Shizuku

class ShellManager(
    private val context: Context,
    private val handler: Handler,
    private val executor: ExecutorService,
) {
    private var hasRoot: Boolean? = null
    private var shizukuPermissionListener: Shizuku.OnRequestPermissionResultListener? = null
    private val shizukuServiceArgs: Shizuku.UserServiceArgs =
        Shizuku.UserServiceArgs(ComponentName(context, ShizukuShellService::class.java))
            .processNameSuffix("shell")
            .tag("shappky_shell")
            .version(SHIZUKU_SERVICE_VERSION)
            .daemon(false)
    private val shizukuServiceConnection: ServiceConnection
    private var shizukuService: IShizukuShellService? = null
    private var isShizukuServiceBound = false
    private var isShizukuServiceBinding = false
    private var shizukuServiceBindingStartedAt = 0L
    private var onShizukuServiceConnected: Runnable? = null

    private val shizukuBinderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku binder received callback started")
        Log.d(
            TAG,
            "Shizuku binder received, version=${safeGetShizukuVersion()}, uid=${safeGetShizukuUid()}",
        )
        val mode = getPermissionMode()
        val permission = hasShizukuPermission()
        Log.d(TAG, "Shizuku binder received state mode=$mode, hasPermission=$permission")
        if (mode == "shizuku" && permission) {
            Log.d(TAG, "Shizuku binder ready in shizuku mode, binding user service")
            bindShizukuService()
        } else {
            Log.d(TAG, "Shizuku binder received but bind skipped, mode=$mode, hasPermission=$permission")
        }
    }

    private val shizukuBinderDeadListener = Shizuku.OnBinderDeadListener {
        Log.w(TAG, "Shizuku binder died")
        shizukuService = null
        isShizukuServiceBound = false
        isShizukuServiceBinding = false
        shizukuServiceBindingStartedAt = 0L
    }

    init {
        shizukuServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.d(
                    TAG,
                    "Shizuku service connected: $name, binderAlive=${service.isBinderAlive}, binderPing=${service.pingBinder()}",
                )
                shizukuService = IShizukuShellService.Stub.asInterface(service)
                isShizukuServiceBound = true
                isShizukuServiceBinding = false
                shizukuServiceBindingStartedAt = 0L
                Log.d(
                    TAG,
                    "Shizuku service state after connect bound=$isShizukuServiceBound, binding=$isShizukuServiceBinding, serviceReady=${shizukuService != null}",
                )
                onShizukuServiceConnected?.let { handler.post(it) }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                Log.d(TAG, "Shizuku service disconnected: $name")
                shizukuService = null
                isShizukuServiceBound = false
                isShizukuServiceBinding = false
                shizukuServiceBindingStartedAt = 0L
            }
        }
        Shizuku.addBinderReceivedListenerSticky(shizukuBinderReceivedListener)
        Shizuku.addBinderDeadListener(shizukuBinderDeadListener)
    }

    private fun getPermissionMode(): String =
        context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            .getString("permissionMode", "shizuku") ?: "shizuku"

    fun setShizukuPermissionListener(listener: Shizuku.OnRequestPermissionResultListener?) {
        shizukuPermissionListener = listener
        Log.d(TAG, "setShizukuPermissionListener listenerSet=${listener != null}")
        if (listener != null) {
            Shizuku.addRequestPermissionResultListener(listener)
            Log.d(TAG, "Shizuku permission result listener registered")
        }
    }

    fun removeShizukuPermissionListener() {
        Log.d(TAG, "removeShizukuPermissionListener listenerSet=${shizukuPermissionListener != null}")
        shizukuPermissionListener?.let {
            Shizuku.removeRequestPermissionResultListener(it)
            Log.d(TAG, "Shizuku permission result listener removed")
        }
        Shizuku.removeBinderReceivedListener(shizukuBinderReceivedListener)
        Shizuku.removeBinderDeadListener(shizukuBinderDeadListener)
        Log.d(TAG, "Shizuku binder listeners removed")
    }

    fun setOnShizukuServiceConnected(listener: Runnable?) {
        onShizukuServiceConnected = listener
    }

    fun hasRootAccess(): Boolean {
        if (hasRoot == null) {
            hasRoot = try {
                val rooted = Shell.getShell().isRoot
                Log.d(TAG, "Root access checked result=$rooted")
                rooted
            } catch (e: Exception) {
                Log.w(TAG, "Root access check failed", e)
                false
            }
        } else {
            Log.d(TAG, "Root access cached result=${hasRoot == true}")
        }
        return hasRoot == true
    }

    fun hasShizukuPermission(): Boolean {
        val ping = Shizuku.pingBinder()
        val permission = if (ping) Shizuku.checkSelfPermission() else PackageManager.PERMISSION_DENIED
        val granted = ping && permission == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "hasShizukuPermission ping=$ping, permission=$permission, granted=$granted")
        return granted
    }

    private fun safeGetShizukuVersion(): Int =
        try {
            Shizuku.getVersion()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Unable to read Shizuku version", e)
            -1
        }

    private fun safeGetShizukuUid(): Int =
        try {
            Shizuku.getUid()
        } catch (e: RuntimeException) {
            Log.w(TAG, "Unable to read Shizuku uid", e)
            -1
        }

    private fun waitForShizukuService(timeoutMs: Long): Boolean {
        if (!hasShizukuPermission()) {
            Log.w(TAG, "Cannot wait for Shizuku service: permission unavailable")
            return false
        }
        Log.d(
            TAG,
            "Waiting for Shizuku service, bound=$isShizukuServiceBound, binding=$isShizukuServiceBinding, ready=${shizukuService != null}",
        )
        bindShizukuService()
        val deadline = System.currentTimeMillis() + timeoutMs
        var waitIterations = 0
        while (shizukuService == null && System.currentTimeMillis() < deadline) {
            waitIterations++
            Log.d(
                TAG,
                "Waiting for Shizuku service iteration=$waitIterations, bound=$isShizukuServiceBound, binding=$isShizukuServiceBinding, remainingMs=${deadline - System.currentTimeMillis()}",
            )
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                isShizukuServiceBinding = false
                shizukuServiceBindingStartedAt = 0L
                Log.w(TAG, "Interrupted while waiting for Shizuku service after iterations=$waitIterations")
                return false
            }
        }
        val ready = shizukuService != null
        if (!ready) {
            Log.w(
                TAG,
                "Timed out waiting for Shizuku service after ${timeoutMs}ms, iterations=$waitIterations, keeping binding=$isShizukuServiceBinding",
            )
        } else {
            Log.d(TAG, "Shizuku service ready after iterations=$waitIterations")
        }
        return ready
    }

    fun checkShellPermissions() {
        val mode = getPermissionMode()
        val ping = Shizuku.pingBinder()
        Log.d(TAG, "checkShellPermissions started mode=$mode, ping=$ping")
        if (ping) {
            val permission = Shizuku.checkSelfPermission()
            Log.d(TAG, "checkShellPermissions Shizuku permission=$permission")
            if (permission != PackageManager.PERMISSION_GRANTED) {
                if (mode == "shizuku") {
                    Log.d(TAG, "Requesting Shizuku permission requestCode=0")
                    Shizuku.requestPermission(0)
                } else {
                    Log.d(TAG, "Shizuku permission missing but mode=$mode, not requesting")
                }
            } else if (mode == "shizuku") {
                Log.d(TAG, "Shizuku permission granted, binding service")
                bindShizukuService()
            } else {
                Log.d(TAG, "Shizuku permission granted but current mode=$mode")
            }
        } else {
            Log.w(TAG, "checkShellPermissions Shizuku binder not available")
        }
    }

    fun hasAnyShellPermission(): Boolean {
        val mode = getPermissionMode()
        if (mode == "shizuku") {
            val granted = hasShizukuPermission()
            Log.d(TAG, "hasAnyShellPermission mode=shizuku result=$granted")
            return granted
        }
        val rooted = hasRootAccess()
        Log.d(TAG, "hasAnyShellPermission mode=root result=$rooted")
        return rooted
    }

    fun isShellCommandReady(): Boolean {
        val mode = getPermissionMode()
        if (mode == "root") {
            val rooted = hasRootAccess()
            Log.d(TAG, "isShellCommandReady mode=root result=$rooted")
            return rooted
        }
        val hasPermission = hasShizukuPermission()
        val ready = hasPermission && shizukuService != null
        Log.d(
            TAG,
            "isShellCommandReady mode=shizuku permission=$hasPermission, bound=$isShizukuServiceBound, binding=$isShizukuServiceBinding, serviceReady=${shizukuService != null}, result=$ready",
        )
        if (hasPermission && !ready && !isShizukuServiceBinding) bindShizukuService()
        return ready
    }

    fun bindShizukuService() {
        val now = System.currentTimeMillis()
        if (isShizukuServiceBinding && shizukuServiceBindingStartedAt > 0L) {
            val bindingAgeMs = now - shizukuServiceBindingStartedAt
            if (bindingAgeMs > SHIZUKU_BIND_STALE_MS) {
                Log.w(TAG, "Shizuku binding is stale ageMs=$bindingAgeMs, allowing rebind")
                isShizukuServiceBinding = false
                shizukuServiceBindingStartedAt = 0L
            }
        }
        Log.d(
            TAG,
            "bindShizukuService requested bound=$isShizukuServiceBound, binding=$isShizukuServiceBinding, serviceReady=${shizukuService != null}",
        )
        if (!Shizuku.pingBinder()) {
            Log.d(TAG, "Skipping Shizuku bind, binder is not ready")
            return
        }
        val hasPermission = hasShizukuPermission()
        if (isShizukuServiceBound || isShizukuServiceBinding || !hasPermission) {
            Log.d(
                TAG,
                "Skipping Shizuku bind, bound=$isShizukuServiceBound, binding=$isShizukuServiceBinding, hasPermission=$hasPermission",
            )
            return
        }
        try {
            isShizukuServiceBinding = true
            shizukuServiceBindingStartedAt = now
            Log.d(TAG, "Binding Shizuku user service, version=${safeGetShizukuVersion()}, uid=${safeGetShizukuUid()}")
            Shizuku.bindUserService(shizukuServiceArgs, shizukuServiceConnection)
        } catch (e: RuntimeException) {
            isShizukuServiceBinding = false
            shizukuServiceBindingStartedAt = 0L
            Log.e(TAG, "Failed to bind Shizuku user service", e)
            e.printStackTrace()
        }
    }

    fun unbindShizukuService() {
        if (!isShizukuServiceBound) {
            shizukuService = null
            isShizukuServiceBinding = false
            shizukuServiceBindingStartedAt = 0L
            return
        }
        try {
            Shizuku.unbindUserService(shizukuServiceArgs, shizukuServiceConnection, true)
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }
        shizukuService = null
        isShizukuServiceBound = false
        isShizukuServiceBinding = false
        shizukuServiceBindingStartedAt = 0L
    }

    fun runShellCommand(command: String, onSuccess: Runnable?) {
        executor.execute {
            val mode = getPermissionMode()
            Log.d(TAG, "runShellCommand started mode=$mode, command=$command")
            var executed = false
            if (mode == "root" && hasRootAccess()) {
                Log.d(TAG, "runShellCommand attempting root execution")
                if (executeRootCommand(command, onSuccess, null)) executed = true
            }
            if (!executed && mode == "shizuku" && hasShizukuPermission()) {
                Log.d(TAG, "runShellCommand attempting Shizuku execution")
                if (executeShizukuCommand(command, onSuccess)) executed = true
            }
            if (!executed) {
                Log.w(TAG, "runShellCommand not executed, posting success callback anyway command=$command")
                onSuccess?.let { handler.post(it) }
            } else {
                Log.d(TAG, "runShellCommand executed successfully command=$command")
            }
        }
    }

    fun runShellCommandWithOutput(command: String, outputProcessor: Consumer<String>) {
        executor.execute {
            var executed = false
            if (getPermissionMode() == "root" && hasRootAccess()) {
                if (executeRootCommand(command, null, outputProcessor)) executed = true
            }
            if (!executed && getPermissionMode() == "shizuku" && hasShizukuPermission()) {
                if (executeShizukuCommandWithOutput(command, outputProcessor)) executed = true
            }
        }
    }

    fun runShellCommandAndGetFullOutput(command: String): String? {
        val mode = getPermissionMode()
        Log.d(TAG, "runShellCommandAndGetFullOutput mode=$mode, command=$command")
        return when {
            mode == "root" && hasRootAccess() -> {
                Log.d(TAG, "Full output command using root")
                executeRootCommandAndGetFullOutput(command)
            }
            mode == "shizuku" && hasShizukuPermission() -> {
                Log.d(TAG, "Full output command using Shizuku")
                executeShizukuCommandAndGetFullOutput(command)
            }
            else -> {
                Log.w(TAG, "Full output command skipped no permission mode=$mode, command=$command")
                null
            }
        }
    }

    private fun executeRootCommand(
        command: String,
        onSuccess: Runnable?,
        outputProcessor: Consumer<String>?,
    ): Boolean =
        try {
            Log.d(TAG, "Root command executing command=$command")
            val result = Shell.cmd(command).exec()
            Log.d(
                TAG,
                "Root command result success=${result.isSuccess}, outLines=${result.out.size}, errLines=${result.err.size}",
            )
            if (outputProcessor != null) {
                result.out.forEach { line -> handler.post { outputProcessor.accept(line) } }
                result.err.forEach { line -> handler.post { outputProcessor.accept("ERROR: $line") } }
            }
            onSuccess?.let { handler.post(it) }
            result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Root command failed command=$command", e)
            e.printStackTrace()
            false
        }

    private fun executeShizukuCommand(command: String, onSuccess: Runnable?): Boolean {
        return try {
            if (!waitForShizukuService(SHIZUKU_BIND_TIMEOUT_MS)) return false
            Log.d(TAG, "Shizuku command executing command=$command")
            shizukuService?.runCommand(command)
            Log.d(TAG, "Shizuku command executed command=$command")
            onSuccess?.let { handler.post(it) }
            true
        } catch (e: RemoteException) {
            Log.e(TAG, "Shizuku command RemoteException command=$command", e)
            e.printStackTrace()
            false
        }
    }

    private fun executeShizukuCommandWithOutput(
        command: String,
        outputProcessor: Consumer<String>,
    ): Boolean {
        return try {
            if (!waitForShizukuService(SHIZUKU_BIND_TIMEOUT_MS)) return false
            Log.d(TAG, "Shizuku command with output executing command=$command")
            val output = shizukuService?.runCommand(command)
            Log.d(TAG, "Shizuku command with output finished outputLength=${output?.length ?: -1}")
            output?.split(Regex("\\r?\\n"))?.forEach { line ->
                handler.post { outputProcessor.accept(line) }
            }
            true
        } catch (e: RemoteException) {
            Log.e(TAG, "Shizuku command with output failed command=$command", e)
            e.printStackTrace()
            false
        }
    }

    private fun executeRootCommandAndGetFullOutput(command: String): String? {
        val output = StringBuilder()
        return try {
            Log.d(TAG, "Root full output command executing command=$command")
            val result = Shell.cmd(command).exec()
            result.out.forEach { line -> output.append(line).append("\n") }
            result.err.forEach { line -> output.append("ERROR: ").append(line).append("\n") }
            val text = output.toString()
            Log.d(
                TAG,
                "Root command finished success=${result.isSuccess}, outLines=${result.out.size}, errLines=${result.err.size}, outputLength=${text.length}",
            )
            text
        } catch (e: Exception) {
            Log.e(TAG, "Root command failed command=$command", e)
            e.printStackTrace()
            null
        }
    }

    private fun executeShizukuCommandAndGetFullOutput(command: String): String? =
        try {
            if (!waitForShizukuService(SHIZUKU_BIND_TIMEOUT_MS)) {
                Log.w(TAG, "Shizuku command skipped because service is not ready command=$command")
                null
            } else {
                Log.d(TAG, "Shizuku full output command executing command=$command")
                val output = shizukuService?.runCommand(command)
                Log.d(TAG, "Shizuku command finished outputLength=${output?.length ?: -1}")
                output
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Shizuku command failed command=$command", e)
            e.printStackTrace()
            null
        }

    companion object {
        private const val TAG = "ShappkyShell"
        private const val SHIZUKU_SERVICE_VERSION = 1
        private const val SHIZUKU_BIND_TIMEOUT_MS = 3000L
        private const val SHIZUKU_BIND_STALE_MS = 90000L
    }
}
