package com.yn.shappky.ui.dialogs

import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yn.shappky.R
import com.yn.shappky.data.models.AppModel
import com.yn.shappky.utils.ProtectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProtectedAppsDialog(
  loadAllApps: ((List<AppModel>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
  onSave: (Set<String>) -> Unit,
) {
  val context = LocalContext.current
  val pm = context.packageManager
  val focusManager = LocalFocusManager.current

  var query by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(true) }
  val allApps = remember { mutableStateListOf<AppModel>() }
  var selectedPackages by remember { mutableStateOf(ProtectionManager.getProtectedApps(context)) }

  // Special package targets
  val launcherPackage = remember {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
  }

  val keyboardPackage = remember {
    val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    if (raw != null && raw.contains("/")) raw.split("/")[0] else null
  }

  val wallpaperPackages = remember {
    val packages = mutableSetOf<String>()
    try {
      val wm = WallpaperManager.getInstance(context)
      wm.wallpaperInfo?.packageName?.let { packages.add(it) }
      if (Build.VERSION.SDK_INT >= 34) {
        wm.getWallpaperInfo(WallpaperManager.FLAG_LOCK)?.packageName?.let { packages.add(it) }
      }
    } catch (e: Exception) {
    }
    packages
  }

  var activeWidgetPackages by remember { mutableStateOf(emptySet<String>()) }
  var regexText by remember { mutableStateOf("") }
  var showUserApps by remember { mutableStateOf(true) }
  var showSystemApps by remember { mutableStateOf(true) }
  var showPersistentApps by remember { mutableStateOf(true) }
  var isMenuExpanded by remember { mutableStateOf(false) }
  val androidPackages = remember(allApps.size) {
    allApps
      .filter { it.packageName.startsWith("com.android.") || it.packageName.startsWith("android.") || it.packageName == "android" }
      .map { it.packageName }
  }

  LaunchedEffect(Unit) {
    regexText = com.yn.shappky.utils.ProtectionManager.getProtectedRegex(context)
    loadAllApps { result ->
      allApps.clear()
      allApps.addAll(result)
      isLoading = false
    }

    withContext(Dispatchers.IO) {
      try {
        val shellManager = com.yn.shappky.utils.ShellManager(
          context,
          android.os.Handler(android.os.Looper.getMainLooper()),
          java.util.concurrent.Executors.newSingleThreadExecutor(),
        )
        val output = shellManager.runShellCommandAndGetFullOutput("dumpsys appwidget") ?: ""
        val activePackages = mutableSetOf<String>()
        val regex = Regex("cmp:ComponentInfo\\{([^/]+)/")
        val legacyRegex = Regex("provider=ComponentInfo\\{([^/]+)/")

        var inWidgetsSection = false
        for (line in output.split('\n')) {
          val trimmed = line.trim()
          if (trimmed == "Widgets:" || trimmed == "AppWidgetIds:") {
            inWidgetsSection = true
            continue
          } else if (line.isNotEmpty() && !line.startsWith(" ") && !line.startsWith("\t")) {
            if (inWidgetsSection && !line.contains("Widgets") && !line.contains("AppWidgetIds")) {
              inWidgetsSection = false
            }
          }
          if (inWidgetsSection) {
            val match = regex.find(line) ?: legacyRegex.find(line)
            if (match != null) {
              activePackages.add(match.groupValues[1])
            }
          }
        }
        activeWidgetPackages = activePackages
      } catch (e: Exception) {
        // Ignore
      }
    }
  }

  LaunchedEffect(regexText) {
    if (regexText.isNotBlank() && allApps.isNotEmpty()) {
      val patterns = regexText.split("|").map { it.trim() }.filter { it.isNotEmpty() }
      val matchingApps = allApps.filter { app ->
        var matches = false
        for (pattern in patterns) {
          try {
            val regex = pattern.replace(".", "\\.").replace("*", ".*").toRegex()
            if (regex.matches(app.packageName)) {
              matches = true
              break
            }
          } catch (e: Exception) {
            if (pattern.endsWith(".*") && app.packageName.startsWith(pattern.removeSuffix(".*"))) {
              matches = true
              break
            } else if (app.packageName == pattern) {
              matches = true
              break
            }
          }
        }
        matches
      }.map { it.packageName }

      if (matchingApps.isNotEmpty()) {
        selectedPackages = selectedPackages + matchingApps
      }
    }
  }

  AlertDialog(
    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.protected_apps_list_title)) },
    text = {
      Column(modifier = Modifier.height(460.dp)) {
        // Search bar
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text(stringResource(R.string.search_apps)) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { isMenuExpanded = true }) {
              Icon(Icons.Default.MoreVert, contentDescription = "Filter")
            }
            androidx.compose.material3.DropdownMenu(
              expanded = isMenuExpanded,
              onDismissRequest = { isMenuExpanded = false },
            ) {
              androidx.compose.material3.DropdownMenuItem(
                text = { Text("User Apps") },
                trailingIcon = { Checkbox(checked = showUserApps, onCheckedChange = { showUserApps = it }) },
                onClick = { showUserApps = !showUserApps },
              )
              androidx.compose.material3.DropdownMenuItem(
                text = { Text("System Apps") },
                trailingIcon = { Checkbox(checked = showSystemApps, onCheckedChange = { showSystemApps = it }) },
                onClick = { showSystemApps = !showSystemApps },
              )
              androidx.compose.material3.DropdownMenuItem(
                text = { Text("Persistent Apps") },
                trailingIcon = { Checkbox(checked = showPersistentApps, onCheckedChange = { showPersistentApps = it }) },
                onClick = { showPersistentApps = !showPersistentApps },
              )
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        )
        Spacer(Modifier.height(8.dp))

        if (isLoading) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else {
          val filtered = allApps.filter { app ->
            val matchesQuery = app.appName.contains(query, ignoreCase = true) || app.packageName.contains(query, ignoreCase = true)
            var matchesFilter = false
            if (app.isPersistentApp && showPersistentApps) {
              matchesFilter = true
            } else if (app.isSystemApp && !app.isPersistentApp && showSystemApps) {
              matchesFilter = true
            } else if (!app.isSystemApp && showUserApps) {
              matchesFilter = true
            }

            matchesQuery && matchesFilter
          }

          val launcherChecked = launcherPackage != null && selectedPackages.contains(launcherPackage)
          val keyboardChecked = keyboardPackage != null && selectedPackages.contains(keyboardPackage)
          val wallpaperChecked = wallpaperPackages.isNotEmpty() && wallpaperPackages.all { selectedPackages.contains(it) }
          val widgetsChecked = activeWidgetPackages.isNotEmpty() && activeWidgetPackages.all { selectedPackages.contains(it) }
          val googleAndroidPackages = allApps.map { it.packageName }.filter { it.startsWith("com.google.android") }
          val googleAndroidServicesChecked = googleAndroidPackages.isNotEmpty() && googleAndroidPackages.all { selectedPackages.contains(it) }
          val androidServicesChecked = androidPackages.isNotEmpty() && androidPackages.all { selectedPackages.contains(it) }

          LazyColumn {
            // Special checkboxes
            item {
              SpecialCheckboxRow(
                text = "Shappky",
                checked = selectedPackages.contains(context.packageName),
                onCheckedChange = { checked ->
                  selectedPackages = if (checked) selectedPackages + context.packageName else selectedPackages - context.packageName
                },
              )
            }
            item {
              SpecialCheckboxRow(
                text = stringResource(R.string.launcher),
                checked = launcherChecked,
                onCheckedChange = { checked ->
                  launcherPackage?.let { pkg ->
                    selectedPackages = if (checked) selectedPackages + pkg else selectedPackages - pkg
                  }
                },
              )
            }
            item {
              val wallpaperText = stringResource(R.string.wallpaper)
              SpecialCheckboxRow(
                text = wallpaperText,
                checked = wallpaperChecked,
                onCheckedChange = { checked ->
                  if (wallpaperPackages.isNotEmpty()) {
                    selectedPackages = if (checked) {
                      selectedPackages + wallpaperPackages
                    } else {
                      selectedPackages - wallpaperPackages
                    }
                  } else {
                    Toast.makeText(context, "$wallpaperText: Not found", Toast.LENGTH_SHORT).show()
                  }
                },
              )
            }
            item {
              SpecialCheckboxRow(
                text = stringResource(R.string.keyboard),
                checked = keyboardChecked,
                onCheckedChange = { checked ->
                  keyboardPackage?.let { pkg ->
                    selectedPackages = if (checked) selectedPackages + pkg else selectedPackages - pkg
                  }
                },
              )
            }

            item {
              SpecialCheckboxRow(
                text = stringResource(R.string.widgets),
                checked = widgetsChecked,
                onCheckedChange = { checked ->
                  selectedPackages = if (checked) {
                    selectedPackages + activeWidgetPackages
                  } else {
                    selectedPackages - activeWidgetPackages
                  }
                },
              )
            }
            item {
              SpecialCheckboxRow(
                text = stringResource(R.string.android_services),
                checked = androidServicesChecked,
                onCheckedChange = { checked ->
                  selectedPackages = if (checked) {
                    selectedPackages + androidPackages
                  } else {
                    selectedPackages - androidPackages.toSet()
                  }
                },
              )
            }
            item {
              SpecialCheckboxRow(
                text = stringResource(R.string.google_android_services),
                checked = googleAndroidServicesChecked,
                onCheckedChange = { checked ->
                  selectedPackages = if (checked) {
                    selectedPackages + googleAndroidPackages
                  } else {
                    selectedPackages - googleAndroidPackages.toSet()
                  }
                },
              )
            }
            item {
              OutlinedTextField(
                value = regexText,
                onValueChange = { regexText = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("com.miui.*|com.xiaomi.*|com.lbe.security.miui") },
                label = { Text("Regex Pattern") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
              )
            }

            item {
              HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
              )
            }

            // Normal app list
            items(filtered, key = { it.packageName }) { app ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    selectedPackages = if (selectedPackages.contains(app.packageName)) {
                      selectedPackages - app.packageName
                    } else {
                      selectedPackages + app.packageName
                    }
                  }
                  .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                DrawableIcon(app.appIcon)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = app.appName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                  )
                  Text(
                    text = app.packageName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Checkbox(
                  checked = selectedPackages.contains(app.packageName),
                  onCheckedChange = { checked ->
                    selectedPackages = if (checked) {
                      selectedPackages + app.packageName
                    } else {
                      selectedPackages - app.packageName
                    }
                  },
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = {
            val defaultSet = ProtectionManager.getDefaultProtectedApps(context)
            selectedPackages = defaultSet
          },
        ) {
          Text(stringResource(R.string.reset))
        }
        Row {
          TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.cancel))
          }
          Spacer(Modifier.width(8.dp))
          TextButton(
            onClick = {
              com.yn.shappky.utils.ProtectionManager.saveProtectedRegex(context, regexText)
              onSave(selectedPackages)
            },
          ) {
            Text(stringResource(R.string.save))
          }
        }
      }
    },
  )
}

@Composable
private fun SpecialCheckboxRow(
  text: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCheckedChange(!checked) }
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Spacer(Modifier.width(12.dp))
    Text(
      text = text,
      modifier = Modifier.weight(1f),
      style = MaterialTheme.typography.bodyLarge,
    )
    Checkbox(
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
  }
}

@Composable
private fun DrawableIcon(drawable: Drawable) {
  AndroidView(
    factory = { context ->
      ImageView(context).apply {
        layoutParams = ViewGroup.LayoutParams(48, 48)
        scaleType = ImageView.ScaleType.FIT_CENTER
      }
    },
    update = { imageView -> imageView.setImageDrawable(drawable) },
    modifier = Modifier.size(48.dp),
  )
}
