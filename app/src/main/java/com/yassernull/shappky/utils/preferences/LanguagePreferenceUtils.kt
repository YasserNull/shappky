package com.yassernull.shappky.utils

fun getLanguageIndex(language: String?): Int = when (language) {
  "en" -> 1
  "ar" -> 2
  "zh-CN", "zh" -> 3
  "ru" -> 4
  "hi" -> 5
  "de" -> 6
  "fr" -> 7
  else -> 0
}

fun languageFromIndex(index: Int): String = when (index) {
  1 -> "en"
  2 -> "ar"
  3 -> "zh-CN"
  4 -> "ru"
  5 -> "hi"
  6 -> "de"
  7 -> "fr"
  else -> "system"
}

fun getLanguageLabel(language: String?, options: Array<String>): String {
  val index = getLanguageIndex(language)
  return if (index in options.indices) options[index] else options[0]
}
