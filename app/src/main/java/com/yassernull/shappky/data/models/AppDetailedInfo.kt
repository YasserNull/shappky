package com.yassernull.shappky.data.models

data class AppDetailedInfo(
  val app: AppModel,
  val pid: String,
  val user: String,
  val isForeground: Boolean,
  val cpuUsage: String,
  val threads: String,
  val totalRamKb: Long,
  val processes: List<ProcessInfo>,
)

data class ProcessInfo(
  val name: String,
  val pid: String,
  val ramKb: Long,
  val user: String = "-",
  val cpuPercent: Double = 0.0,
)
