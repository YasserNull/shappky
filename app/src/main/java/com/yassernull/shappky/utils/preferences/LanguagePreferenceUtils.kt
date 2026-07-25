package com.yassernull.shappky.utils

fun getLanguageIndex(language: String?): Int = when (language) {
  "en" -> 1
  "ar" -> 2
  else -> 0
}

fun languageFromIndex(index: Int): String = when (index) {
  1 -> "en"
  2 -> "ar"
  else -> "system"
}

fun getLanguageLabel(language: String?, options: Array<String>): String {
  val index = getLanguageIndex(language)
  return if (index in options.indices) options[index] else options[0]
}
