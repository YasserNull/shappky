package com.yn.shappky.utils

import android.os.Handler
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

data class RamState(
  val usedKb: Int = 0,
  val totalKb: Int = 0,
) {
  val progress: Float
    get() = if (totalKb > 0) usedKb.toFloat() / totalKb else 0f
}

class RamMonitor(
  private val handler: Handler,
  private var refreshIntervalMs: Long = DEFAULT_REFRESH_INTERVAL_MS,
  private val onUpdate: (RamState) -> Unit,
) {
  private var isMonitoring = false
  private var ramUsageRunnable: Runnable? = null

  fun startMonitoring() {
    if (isMonitoring) return
    isMonitoring = true
    ramUsageRunnable = object : Runnable {
      override fun run() {
        if (!isMonitoring) return
        readRamState()?.let(onUpdate)
        handler.postDelayed(this, refreshIntervalMs)
      }
    }
    handler.post(requireNotNull(ramUsageRunnable))
  }

  fun setRefreshIntervalMs(intervalMs: Long) {
    refreshIntervalMs = intervalMs.coerceAtLeast(1L)
  }

  fun stopMonitoring() {
    isMonitoring = false
    ramUsageRunnable?.let { handler.removeCallbacks(it) }
    ramUsageRunnable = null
  }

  private fun readRamState(): RamState? {
    return try {
      val process = Runtime.getRuntime().exec("cat /proc/meminfo")
      BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
        var memMax = 0
        var memFree = 0
        repeat(3) {
          val line = reader.readLine() ?: return@repeat
          when {
            line.startsWith("MemTotal") -> memMax = line.split(Regex("\\s+"))[1].toInt()
            line.startsWith("MemAvailable") -> memFree = line.split(Regex("\\s+"))[1].toInt()
          }
        }
        process.waitFor()
        if (memMax > 0 && memFree >= 0) RamState(memMax - memFree, memMax) else null
      }
    } catch (e: IOException) {
      e.printStackTrace()
      null
    } catch (e: NumberFormatException) {
      e.printStackTrace()
      null
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
      e.printStackTrace()
      null
    }
  }

  private companion object {
    const val DEFAULT_REFRESH_INTERVAL_MS = 1000L
  }
}
