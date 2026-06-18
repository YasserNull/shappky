package com.yn.shappky.utils

fun getThemeIndex(theme: String?): Int =
    when (theme) {
        "white" -> 1
        "black" -> 2
        else -> 0
    }

fun themeFromIndex(index: Int): String =
    when (index) {
        1 -> "white"
        2 -> "black"
        else -> "dark"
    }

fun getThemeLabel(theme: String?, options: Array<String>): String {
    val index = getThemeIndex(theme)
    return if (index in options.indices) options[index] else options[0]
}
