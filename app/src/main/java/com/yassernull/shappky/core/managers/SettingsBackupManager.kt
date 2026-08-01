package com.yassernull.shappky.core.managers

import android.content.Context
import android.net.Uri
import android.util.Log
import com.yassernull.shappky.core.preferences.PREFERENCES_NAME
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

object SettingsBackupManager {
  private const val TAG = "SettingsBackup"
  const val EXPORT_FILE_NAME = "shappky_preferences.xml"

  fun exportSettings(context: Context, uri: Uri): Boolean {
    return try {
      val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      val all = prefs.all ?: return false
      val sb = StringBuilder()
      sb.append("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n")
      sb.append("<map>\n")
      for ((key, value) in all) {
        sb.append(serializeEntry(key, value))
      }
      sb.append("</map>\n")
      context.contentResolver.openOutputStream(uri)?.use { out ->
        out.write(sb.toString().toByteArray(Charsets.UTF_8))
        true
      } ?: false
    } catch (e: Exception) {
      Log.e(TAG, "Export failed", e)
      false
    }
  }

  fun importSettings(context: Context, uri: Uri): Boolean {
    return try {
      val xml = context.contentResolver.openInputStream(uri)
        ?.use { it.readBytes().toString(Charsets.UTF_8) }
        ?: return false
      if (!xml.contains("<map")) return false
      val entries = parsePreferencesXml(xml)
      if (entries.isEmpty()) return false

      val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
      val editor = prefs.edit().clear()
      for ((key, value) in entries) {
        when (value) {
          is String -> editor.putString(key, value)
          is Int -> editor.putInt(key, value)
          is Long -> editor.putLong(key, value)
          is Float -> editor.putFloat(key, value)
          is Boolean -> editor.putBoolean(key, value)
          is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
        }
      }
      editor.apply()
      true
    } catch (e: Exception) {
      Log.e(TAG, "Import failed", e)
      false
    }
  }

  private fun serializeEntry(key: String, value: Any?): String {
    val escapedKey = escapeAttribute(key)
    return when (value) {
      is String -> "<string name=\"$escapedKey\">${escapeText(value)}</string>\n"
      is Int -> "<int name=\"$escapedKey\" value=\"$value\" />\n"
      is Long -> "<long name=\"$escapedKey\" value=\"$value\" />\n"
      is Float -> "<float name=\"$escapedKey\" value=\"$value\" />\n"
      is Boolean -> "<boolean name=\"$escapedKey\" value=\"$value\" />\n"
      is Set<*> -> {
        val items = value.joinToString("") { "<string>${escapeText(it.toString())}</string>" }
        "<set name=\"$escapedKey\">$items</set>\n"
      }
      else -> ""
    }
  }

  private fun parsePreferencesXml(xml: String): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(StringReader(xml))
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
      if (event == XmlPullParser.START_TAG) {
        when (parser.name) {
          "string" -> {
            val name = parser.getAttributeValue(null, "name")
            if (name != null) {
              val value = if (parser.next() == XmlPullParser.TEXT) parser.text else ""
              result[name] = value
            }
          }
          "int" -> parser.getAttributeValue(null, "name")?.let { name ->
            result[name] = parser.getAttributeValue(null, "value")?.toIntOrNull() ?: 0
          }
          "long" -> parser.getAttributeValue(null, "name")?.let { name ->
            result[name] = parser.getAttributeValue(null, "value")?.toLongOrNull() ?: 0L
          }
          "float" -> parser.getAttributeValue(null, "name")?.let { name ->
            result[name] = parser.getAttributeValue(null, "value")?.toFloatOrNull() ?: 0f
          }
          "boolean" -> parser.getAttributeValue(null, "name")?.let { name ->
            result[name] = parser.getAttributeValue(null, "value")?.toBooleanStrictOrNull() ?: false
          }
          "set" -> {
            val name = parser.getAttributeValue(null, "name")
            if (name != null) {
              val items = mutableListOf<String>()
              var e = parser.next()
              while (!(e == XmlPullParser.END_TAG && parser.name == "set")) {
                if (e == XmlPullParser.START_TAG && parser.name == "string") {
                  if (parser.next() == XmlPullParser.TEXT) {
                    items.add(parser.text)
                  }
                }
                e = parser.next()
              }
              result[name] = items.toSet()
            }
          }
        }
      }
      event = parser.next()
    }
    return result
  }

  private fun escapeText(text: String): String = buildString(text.length) {
    for (c in text) {
      when (c) {
        '&' -> append("&amp;")
        '<' -> append("&lt;")
        '>' -> append("&gt;")
        else -> append(c)
      }
    }
  }

  private fun escapeAttribute(text: String): String = buildString(text.length) {
    for (c in text) {
      when (c) {
        '&' -> append("&amp;")
        '<' -> append("&lt;")
        '>' -> append("&gt;")
        '"' -> append("&quot;")
        '\'' -> append("&apos;")
        else -> append(c)
      }
    }
  }
}
