package com.yn.shappky.utils

fun getLanguageIndex(language: String?): Int = when (language) {
  "ar" -> 1
  "en" -> 2
  "zh" -> 3
  "hi" -> 4
  "fr" -> 5
  "ja" -> 6
  "es" -> 7
  else -> 0
}

fun languageFromIndex(index: Int): String = when (index) {
  1 -> "ar"
  2 -> "en"
  3 -> "zh"
  4 -> "hi"
  5 -> "fr"
  6 -> "ja"
  7 -> "es"
  else -> "system"
}

fun getLanguageLabel(language: String?, options: Array<String>): String {
  val index = getLanguageIndex(language)
  return if (index in options.indices) options[index] else options[0]
}
