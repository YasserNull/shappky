package com.yassernull.shappky.core.managers

import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale

data class PsEntry(
  val packageName: String,
  val rssKb: Long,
)

fun parsePsOutputToEntries(output: String): List<PsEntry> {
  val entries = mutableListOf<PsEntry>()
  BufferedReader(StringReader(output)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      val parts = line.trim().split(Regex("\\s+"))
      if (parts.size >= 2) {
        val rawPackageName = parts[1].trim()
        val packageName = if (rawPackageName.contains(":")) rawPackageName.substringBefore(":") else rawPackageName
        val rssKb = parts[0].trim().toLongOrNull() ?: 0L
        if (packageName.isNotEmpty() && packageName.contains(".") && !packageName.startsWith("ERROR:")) {
          entries.add(PsEntry(packageName, rssKb))
        }
      }
      line = reader.readLine()
    }
  }
  return entries
}

fun aggregateByPackage(entries: List<PsEntry>): Map<String, Long> {
  val map = mutableMapOf<String, Long>()
  for (entry in entries) {
    map[entry.packageName] = (map[entry.packageName] ?: 0L) + entry.rssKb
  }
  return map
}

fun parsePsOutputToProcessInfos(
  output: String,
  packageName: String,
): List<com.yassernull.shappky.data.models.ProcessInfo> {
  val processes = mutableListOf<com.yassernull.shappky.data.models.ProcessInfo>()
  BufferedReader(StringReader(output)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      val parts = line.trim().split(Regex("\\s+"))
      if (parts.size >= 4 && !line.startsWith("ERROR:")) {
        val pid = parts[0]
        val rss = parts[2].toLongOrNull() ?: 0L
        val name = parts[3]
        if (name.startsWith(packageName)) {
          processes.add(com.yassernull.shappky.data.models.ProcessInfo(name, pid, rss))
        }
      }
      line = reader.readLine()
    }
  }
  return processes
}

fun parseCpuInfoOutput(cpuOutput: String): Double {
  var totalCpu = 0.0
  BufferedReader(StringReader(cpuOutput)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      val parts = line.trim().split(Regex("\\s+"))
      if (parts.isNotEmpty() && parts[0].endsWith("%")) {
        val percentStr = parts[0].removeSuffix("%")
        totalCpu += percentStr.toDoubleOrNull() ?: 0.0
      }
      line = reader.readLine()
    }
  }
  return totalCpu
}

fun parseStatForThreads(statOutput: String): Int {
  if (statOutput.startsWith("ERROR")) return 0
  val statParts = statOutput.trim().split(" ")
  return if (statParts.size >= 20) statParts[19].toIntOrNull() ?: 0 else 0
}

fun parseMemoryToKb(ram: String?): Long {
  if (ram.isNullOrEmpty() || ram == "-") return 0
  val normalizedRam = ram.trim().uppercase(Locale.getDefault())
  return try {
    when {
      normalizedRam.endsWith("KB") -> normalizedRam.replace("KB", "").trim().toFloat().toLong()
      normalizedRam.endsWith("MB") -> (normalizedRam.replace("MB", "").trim().toFloat() * 1024).toLong()
      normalizedRam.endsWith("GB") -> (normalizedRam.replace("GB", "").trim().toFloat() * 1024 * 1024).toLong()
      else -> 0
    }
  } catch (_: NumberFormatException) {
    0
  }
}
