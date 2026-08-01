package com.yassernull.shappky.core.managers

import android.os.Process
import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale
import java.util.regex.Pattern

data class PsEntry(
  val packageName: String,
  val rssKb: Long,
  val uid: String,
)

fun parsePsOutputToEntries(output: String): List<PsEntry> {
  val entries = mutableListOf<PsEntry>()
  BufferedReader(StringReader(output)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      val parts = line.trim().split(Regex("\\s+"))
      if (parts.size >= 2 && !line.startsWith("ERROR:")) {
        val rawName = parts[1].trim()
        val rssKb = parts[0].trim().toLongOrNull() ?: 0L
        val uid = if (parts.size >= 3) parts[2].trim() else ""
        val isAppUser = uid.toLongOrNull()?.let { it >= Process.FIRST_APPLICATION_UID } ?: false
        val packageName = if (rawName.contains(".")) rawName.substringBefore(":") else ""
        if (isAppUser && rawName.isNotEmpty()) {
          entries.add(PsEntry(packageName, rssKb, uid))
        }
      }
      line = reader.readLine()
    }
  }
  return entries
}

fun aggregateByPackage(entries: List<PsEntry>): Map<String, Long> {
  val uidToPackage = mutableMapOf<String, String>()
  for (entry in entries) {
    if (entry.packageName.isNotEmpty() && entry.uid.isNotEmpty()) {
      uidToPackage.putIfAbsent(entry.uid, entry.packageName)
    }
  }
  val map = mutableMapOf<String, Long>()
  for (entry in entries) {
    val packageName = entry.packageName.ifEmpty { uidToPackage[entry.uid] ?: "" }
    if (packageName.isNotEmpty()) {
      map[packageName] = (map[packageName] ?: 0L) + entry.rssKb
    }
  }
  return map
}

fun parsePsOutputToProcessInfos(
  output: String,
  packageName: String,
  uid: String? = null,
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
        val user = if (parts.size >= 2) parts[1] else ""
        val matchesByUid = uid != null &&
          parts.size >= 5 &&
          parts[4] == uid &&
          APP_USER_NAME_PATTERN.matcher(user).matches()
        val matchesByName = isProcessOfPackage(name, packageName)
        if (matchesByUid || matchesByName) {
          processes.add(com.yassernull.shappky.data.models.ProcessInfo(name, pid, rss))
        }
      }
      line = reader.readLine()
    }
  }
  return processes
}

private val APP_USER_NAME_PATTERN = Pattern.compile("^u\\d+_")

fun isProcessOfPackage(processName: String, packageName: String): Boolean = Regex("^" + Pattern.quote(packageName) + "(?![A-Za-z]).*$").matches(processName)

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
