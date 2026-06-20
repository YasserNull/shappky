package com.yn.shappky.shizuku

import android.content.Context
import android.util.Log
import com.yn.shappky.IShizukuShellService
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class ShizukuShellService : IShizukuShellService.Stub {
    constructor() {
        Log.d(TAG, "ShizukuShellService created with default constructor")
    }

    constructor(context: Context) {
        Log.d(TAG, "ShizukuShellService created with context constructor")
    }

    override fun runCommand(command: String): String {
        Log.d(TAG, "runCommand: $command")
        var process: Process? = null
        val output = StringBuilder()
        val startTime = System.currentTimeMillis()
        try {
            Log.d(TAG, "Starting shell process cwd=/ command=$command")
            val processBuilder = ProcessBuilder("sh", "-c", command).directory(File("/"))
            process = processBuilder.start()
            val proc = process
            val stdout = StringBuilder()
            val stderr = StringBuilder()
            val outThread = Thread { readStream(proc.inputStream, stdout, false) }
            val errThread = Thread { readStream(proc.errorStream, stderr, true) }

            outThread.start()
            errThread.start()
            val exitCode = process.waitFor()
            outThread.join()
            errThread.join()

            if (stdout.isNotEmpty()) output.append(stdout)
            if (stderr.isNotEmpty()) output.append(stderr)
            Log.d(
                TAG,
                "runCommand finished exitCode=$exitCode, stdoutLength=${stdout.length}, stderrLength=${stderr.length}, stdoutLines=${stdout.lineCount()}, stderrLines=${stderr.lineCount()}, durationMs=${System.currentTimeMillis() - startTime}",
            )
        } catch (e: IOException) {
            Log.e(TAG, "runCommand IOException command=$command", e)
            output.append("ERROR: ").append(e.message).append("\n")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.e(TAG, "runCommand interrupted command=$command", e)
            output.append("ERROR: ").append(e.message).append("\n")
        } finally {
            Log.d(TAG, "Destroying shell process command=$command")
            process?.destroy()
        }
        Log.d(TAG, "runCommand returning outputLength=${output.length}, durationMs=${System.currentTimeMillis() - startTime}")
        return output.toString()
    }

    override fun destroy() {
        Log.w(TAG, "ShizukuShellService destroy requested")
        System.exit(0)
    }

    companion object {
        private const val TAG = "ShappkyShizukuSvc"

        private fun StringBuilder.lineCount(): Int = if (isEmpty()) 0 else count { it == '\n' }

        private fun readStream(stream: InputStream, output: StringBuilder, isError: Boolean) {
            try {
                var lineCount = 0
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        lineCount++
                        if (isError) {
                            output.append("ERROR: ").append(line).append("\n")
                        } else {
                            output.append(line).append("\n")
                        }
                        line = reader.readLine()
                    }
                }
                Log.d(TAG, "readStream completed isError=$isError, lineCount=$lineCount, outputLength=${output.length}")
            } catch (e: IOException) {
                Log.e(TAG, "readStream failed isError=$isError", e)
                if (isError) {
                    output.append("ERROR: ").append(e.message).append("\n")
                } else {
                    output.append(e.message).append("\n")
                }
            }
        }
    }
}
