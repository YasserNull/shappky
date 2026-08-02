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
  val cpuPercent: Double,
)

data class PackageUsage(
  val ramKb: Long,
  val cpuPercent: Double,
)

fun parsePsOutputToEntries(output: String): List<PsEntry> {
  val entries = mutableListOf<PsEntry>()
  BufferedReader(StringReader(output)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      if (!line.startsWith("ERROR:")) {
        val fields = line.trim().split(Regex("\\s+"))
        var index = 0
        while (index + 3 < fields.size) {
          val cpuPercent = fields[index].toDoubleOrNull() ?: 0.0
          val rssKb = fields[index + 1].toLongOrNull() ?: 0L
          val rawName = fields[index + 2].trim()
          val uid = fields[index + 3].trim()
          val isAppUser = uid.toLongOrNull()?.let { it >= Process.FIRST_APPLICATION_UID } ?: false
          val packageName = if (rawName.contains(".")) rawName.substringBefore(":") else ""
          if (isAppUser && rawName.isNotEmpty()) {
            entries.add(PsEntry(packageName, rssKb, uid, cpuPercent))
          }
          index += 4
        }
      }
      line = reader.readLine()
    }
  }
  return entries
}

fun aggregateByPackage(entries: List<PsEntry>): Map<String, PackageUsage> {
  val uidToPackage = mutableMapOf<String, String>()
  for (entry in entries) {
    if (entry.packageName.isNotEmpty() && entry.uid.isNotEmpty()) {
      uidToPackage.putIfAbsent(entry.uid, entry.packageName)
    }
  }
  val map = mutableMapOf<String, PackageUsage>()
  for (entry in entries) {
    val packageName = entry.packageName.ifEmpty { uidToPackage[entry.uid] ?: "" }
    if (packageName.isNotEmpty()) {
      val current = map[packageName] ?: PackageUsage(0L, 0.0)
      map[packageName] = PackageUsage(
        ramKb = current.ramKb + entry.rssKb,
        cpuPercent = current.cpuPercent + entry.cpuPercent,
      )
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
  val targetUid = uid?.toLongOrNull()
  BufferedReader(StringReader(output)).use { reader ->
    var line = reader.readLine()
    while (line != null) {
      if (!line.startsWith("ERROR:")) {
        val fields = line.trim().split(Regex("\\s+"))
        var index = 0
        while (index + 5 < fields.size) {
          val cpuPercent = fields[index].toDoubleOrNull() ?: 0.0
          val pid = fields[index + 1]
          val user = fields[index + 2]
          val rss = fields[index + 3].toLongOrNull() ?: 0L
          val name = fields[index + 4]
          val rawUid = fields[index + 5].toLongOrNull()
          val matchesByUid = targetUid != null &&
            (
              rawUid == targetUid ||
                androidUserNameToUid(user) == targetUid
              )
          val matchesByName = isProcessOfPackage(name, packageName)
          if (matchesByUid || matchesByName) {
            processes.add(com.yassernull.shappky.data.models.ProcessInfo(name, pid, rss, user, cpuPercent))
          }
          index += 6
        }
      }
      line = reader.readLine()
    }
  }
  return processes
}

private val ANDROID_USER_NAME_PATTERN = Pattern.compile("^u(\\d+)_a(\\d+)$")

fun androidUserNameToUid(user: String): Long? {
  val numeric = user.toLongOrNull()
  if (numeric != null) return numeric
  val match = ANDROID_USER_NAME_PATTERN.matcher(user)
  return if (match.matches()) {
    match.group(1).toLong() * 100000L + 10000L + match.group(2).toLong()
  } else {
    null
  }
}

fun isProcessOfPackage(processName: String, packageName: String): Boolean = Regex("^" + Pattern.quote(packageName) + "(?![A-Za-z0-9]).*$").matches(processName)

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
