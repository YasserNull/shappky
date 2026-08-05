package com.yassernull.shappky.core.managers

import android.content.pm.PackageManager
import android.os.Process
import java.io.BufferedReader
import java.io.StringReader
import java.util.Locale
import java.util.regex.Pattern

const val PS_ALL_PROCESSES_COMMAND = "ps -A -o %cpu,pid,user,rss,name,uid"

fun psAllProcessesCommand(): String = "${ShellManager.TOYBOX_PATH} $PS_ALL_PROCESSES_COMMAND"

data class PsProcessEntry(
  val name: String,
  val pid: String,
  val user: String,
  val rssKb: Long,
  val uid: Long?,
  val cpuPercent: Double,
)

data class PackageUsage(
  val ramKb: Long,
  val cpuPercent: Double,
)

fun parsePsOutputToProcessEntries(output: String): List<PsProcessEntry> {
  val entries = mutableListOf<PsProcessEntry>()
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
          val rssKb = fields[index + 3].toLongOrNull() ?: 0L
          val name = fields[index + 4]
          val uid = fields[index + 5].toLongOrNull()
          entries.add(PsProcessEntry(name, pid, user, rssKb, uid, cpuPercent))
          index += 6
        }
      }
      line = reader.readLine()
    }
  }
  return entries
}

fun parsePsOutputToProcessInfos(
  output: String,
  packageName: String,
  uid: String? = null,
): List<com.yassernull.shappky.data.models.ProcessInfo> {
  val targetUid = uid?.toLongOrNull()
  val canMatchByUid = targetUid != null && targetUid >= Process.FIRST_APPLICATION_UID
  return parsePsOutputToProcessEntries(output)
    .filter { entry ->
      val matchesByUid = canMatchByUid &&
        (
          entry.uid == targetUid ||
            androidUserNameToUid(entry.user) == targetUid
          )
      val matchesByName = isProcessOfPackage(entry.name, packageName)
      matchesByUid || matchesByName
    }
    .map { entry -> com.yassernull.shappky.data.models.ProcessInfo(entry.name, entry.pid, entry.rssKb, entry.user, entry.cpuPercent) }
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

fun aggregatePsOutputToPackages(
  output: String,
  pm: PackageManager,
  installedPackages: Set<String>? = null,
): Map<String, PackageUsage> {
  val packageNames = installedPackages
    ?: pm.getInstalledApplications(0).mapTo(HashSet()) { it.packageName }
  val map = mutableMapOf<String, PackageUsage>()
  for (entry in parsePsOutputToProcessEntries(output)) {
    val packageName = resolvePackageForEntry(entry, pm, packageNames) ?: continue
    val current = map[packageName] ?: PackageUsage(0L, 0.0)
    map[packageName] = PackageUsage(
      ramKb = current.ramKb + entry.rssKb,
      cpuPercent = current.cpuPercent + entry.cpuPercent,
    )
  }
  return map
}

private fun resolvePackageForEntry(
  entry: PsProcessEntry,
  pm: PackageManager,
  installedPackages: Set<String>,
): String? {
  val uid = entry.uid ?: androidUserNameToUid(entry.user)
  val packagesForUid = uid?.let {
    if (it < Process.FIRST_APPLICATION_UID) {
      null
    } else {
      try {
        pm.getPackagesForUid(it.toInt())
      } catch (_: Exception) {
        null
      }
    }
  }
  val byUidPrefix = packagesForUid?.firstOrNull { pkg -> entry.name.startsWith(pkg) }
  val byName = resolvePackageForName(entry.name, installedPackages)
  return when {
    byUidPrefix != null -> byUidPrefix
    byName != null -> byName
    else -> packagesForUid?.firstOrNull()
  }
}

private fun resolvePackageForName(name: String, installedPackages: Set<String>): String? {
  val base = name.substringBefore(":")
  if (base in installedPackages) return base
  return installedPackages.firstOrNull { pkg ->
    name.length >= pkg.length + 1 && name.startsWith(pkg) && !name[pkg.length].isLetterOrDigit()
  }
}
