package com.yassernull.shappky.core

enum class AppTheme {
  LIGHT,
  DARK,
  SYSTEM,
  ;

  fun isDarkTheme(): Boolean = this == DARK
}
