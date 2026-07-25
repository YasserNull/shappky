package com.yassernull.shappky.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R

@Composable
fun ProtectedAppsSpecialSection(
  selectedPackages: Set<String>,
  onToggleSelf: (Boolean) -> Unit,
  launcherPackage: String?,
  launcherChecked: Boolean,
  onToggleLauncher: (Boolean) -> Unit,
  keyboardPackage: String?,
  keyboardChecked: Boolean,
  onToggleKeyboard: (Boolean) -> Unit,
  wallpaperPackages: Set<String>,
  wallpaperChecked: Boolean,
  onToggleWallpaper: (Boolean) -> Unit,
  activeWidgetPackages: Set<String>,
  widgetsChecked: Boolean,
  onToggleWidgets: (Boolean) -> Unit,
  autoBackgroundPackages: Set<String>,
  autoBackgroundChecked: Boolean,
  onToggleAutoBackground: (Boolean) -> Unit,
  androidPackages: List<String>,
  androidServicesChecked: Boolean,
  onToggleAndroidServices: (Boolean) -> Unit,
  googleAndroidPackages: List<String>,
  googleAndroidServicesChecked: Boolean,
  onToggleGoogleServices: (Boolean) -> Unit,
  regexText: String,
  onRegexChange: (String) -> Unit,
) {
  val context = LocalContext.current

  val selfPackage = context.packageName
  SpecialCheckboxRow(
    text = "Shappky",
    checked = selectedPackages.contains(selfPackage),
    onCheckedChange = onToggleSelf,
  )

  SpecialCheckboxRow(
    text = stringResource(R.string.launcher),
    checked = launcherChecked,
    onCheckedChange = onToggleLauncher,
  )

  val wallpaperText = stringResource(R.string.wallpaper)
  SpecialCheckboxRow(
    text = wallpaperText,
    checked = wallpaperChecked,
    onCheckedChange = { checked ->
      if (wallpaperPackages.isNotEmpty()) {
        onToggleWallpaper(checked)
      } else {
        Toast.makeText(context, "$wallpaperText: Not found", Toast.LENGTH_SHORT).show()
      }
    },
  )

  SpecialCheckboxRow(
    text = stringResource(R.string.keyboard),
    checked = keyboardChecked,
    onCheckedChange = onToggleKeyboard,
  )

  SpecialCheckboxRow(
    text = stringResource(R.string.widgets),
    checked = widgetsChecked,
    onCheckedChange = onToggleWidgets,
  )

  val autoBgText = stringResource(R.string.auto_background_apps)
  SpecialCheckboxRow(
    text = autoBgText,
    checked = autoBackgroundChecked,
    onCheckedChange = { checked ->
      if (autoBackgroundPackages.isNotEmpty()) {
        onToggleAutoBackground(checked)
      } else {
        Toast.makeText(context, "$autoBgText: Not found", Toast.LENGTH_SHORT).show()
      }
    },
  )

  SpecialCheckboxRow(
    text = stringResource(R.string.android_services),
    checked = androidServicesChecked,
    onCheckedChange = onToggleAndroidServices,
  )

  SpecialCheckboxRow(
    text = stringResource(R.string.google_android_services),
    checked = googleAndroidServicesChecked,
    onCheckedChange = onToggleGoogleServices,
  )

  OutlinedTextField(
    value = regexText,
    onValueChange = onRegexChange,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    placeholder = { Text("com.miui.*|com.xiaomi.*|com.lbe.security.miui") },
    label = { Text("Regex Pattern") },
    singleLine = true,
    shape = RoundedCornerShape(12.dp),
  )

  HorizontalDivider(
    modifier = Modifier.padding(vertical = 8.dp),
    color = MaterialTheme.colorScheme.outlineVariant,
  )
}
