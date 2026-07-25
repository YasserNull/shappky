package com.yassernull.shappky.core

enum class AppLanguage {
  SYSTEM,
  ENGLISH,
  ARABIC,
  ;

  companion object {
    fun fromTag(tag: String?): AppLanguage = when (tag) {
      "en" -> ENGLISH
      "ar" -> ARABIC
      else -> SYSTEM
    }

    fun toTag(language: AppLanguage): String? = when (language) {
      ENGLISH -> "en"
      ARABIC -> "ar"
      SYSTEM -> null
    }
  }
}
