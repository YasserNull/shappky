package com.yassernull.shappky.core.managers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellOutputParserTest {

  @Test
  fun testIsProcessOfPackage() {
    // Exact match
    assertTrue(isProcessOfPackage("com.termux", "com.termux"))

    // Sub-process match
    assertTrue(isProcessOfPackage("com.termux:x11", "com.termux"))

    // False matches that were previously broken
    assertFalse(isProcessOfPackage("com.termux.x11", "com.termux"))
    assertFalse(isProcessOfPackage("com.termux.someapp", "com.termux"))

    // Invalid package names
    assertFalse(isProcessOfPackage("com.termux", "termux"))
  }
}
