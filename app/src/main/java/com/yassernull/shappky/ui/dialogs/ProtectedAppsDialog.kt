package com.yassernull.shappky.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yassernull.shappky.R
import com.yassernull.shappky.core.managers.ProtectionManager
import com.yassernull.shappky.core.managers.ShellManager
import com.yassernull.shappky.data.models.AppModel
import com.yassernull.shappky.ui.components.ProtectedAppsAppList
import com.yassernull.shappky.ui.components.ProtectedAppsSearchBar
import com.yassernull.shappky.ui.components.ProtectedAppsSpecialSection
import com.yassernull.shappky.utils.applyRegexToSelectedPackages
import com.yassernull.shappky.utils.collectActiveWidgetPackages
import com.yassernull.shappky.utils.collectAutoBackgroundPackages
import com.yassernull.shappky.utils.getAndroidPackages
import com.yassernull.shappky.utils.getKeyboardPackage
import com.yassernull.shappky.utils.getLauncherPackage
import com.yassernull.shappky.utils.getWallpaperPackages
import java.util.concurrent.Executors

@Composable
fun ProtectedAppsDialog(
  loadAllApps: ((List<AppModel>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
  onSave: (Set<String>) -> Unit,
) {
  val context = LocalContext.current
  val pm = context.packageManager

  var query by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(true) }
  val allApps = remember { mutableStateListOf<AppModel>() }
  var selectedPackages by remember { mutableStateOf(ProtectionManager.getProtectedApps(context)) }

  val launcherPackage = remember { pm.getLauncherPackage() }
  val keyboardPackage = remember { context.getKeyboardPackage() }
  val wallpaperPackages = remember { context.getWallpaperPackages(pm) }

  var activeWidgetPackages by remember { mutableStateOf(emptySet<String>()) }
  var autoBackgroundPackages by remember { mutableStateOf(emptySet<String>()) }
  var regexText by remember { mutableStateOf("") }
  var showUserApps by remember { mutableStateOf(true) }
  var showSystemApps by remember { mutableStateOf(true) }
  var showPersistentApps by remember { mutableStateOf(true) }
  var isMenuExpanded by remember { mutableStateOf(false) }

  val androidPackages = remember(allApps.size) { getAndroidPackages(allApps) }

  fun togglePackage(pkg: String) {
    selectedPackages = if (selectedPackages.contains(pkg)) {
      selectedPackages - pkg
    } else {
      selectedPackages + pkg
    }
  }

  LaunchedEffect(Unit) {
    regexText = ProtectionManager.getProtectedRegex(context)
    loadAllApps { result ->
      allApps.clear()
      allApps.addAll(result)
      isLoading = false
      selectedPackages = applyRegexToSelectedPackages(regexText, allApps, selectedPackages)
    }

    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val executor = Executors.newSingleThreadExecutor()
    val shellManager = ShellManager(context, handler, executor)
    activeWidgetPackages = context.collectActiveWidgetPackages(shellManager)
    autoBackgroundPackages = context.collectAutoBackgroundPackages(shellManager)
  }

  LaunchedEffect(regexText) {
    if (allApps.isNotEmpty()) {
      selectedPackages = applyRegexToSelectedPackages(regexText, allApps, selectedPackages)
    }
  }

  AlertDialog(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.protected_apps_list_title)) },
    text = {
      Column(modifier = Modifier.height(460.dp)) {
        ProtectedAppsSearchBar(
          query = query,
          onQueryChange = { query = it },
          showUserApps = showUserApps,
          onShowUserAppsChange = { showUserApps = it },
          showSystemApps = showSystemApps,
          onShowSystemAppsChange = { showSystemApps = it },
          showPersistentApps = showPersistentApps,
          onShowPersistentAppsChange = { showPersistentApps = it },
          isMenuExpanded = isMenuExpanded,
          onToggleMenu = { isMenuExpanded = true },
          onDismissMenu = { isMenuExpanded = false },
        )
        Spacer(Modifier.height(8.dp))

        if (isLoading) {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        } else {
          val googleAndroidPackages = allApps.map { it.packageName }.filter { it.startsWith("com.google.android") }

          val launcherChecked = launcherPackage != null && selectedPackages.contains(launcherPackage)
          val keyboardChecked = keyboardPackage != null && selectedPackages.contains(keyboardPackage)
          val wallpaperChecked = wallpaperPackages.isNotEmpty() && wallpaperPackages.all { selectedPackages.contains(it) }
          val widgetsChecked = activeWidgetPackages.isNotEmpty() && activeWidgetPackages.all { selectedPackages.contains(it) }
          val googleAndroidServicesChecked = googleAndroidPackages.isNotEmpty() && googleAndroidPackages.all { selectedPackages.contains(it) }
          val androidServicesChecked = androidPackages.isNotEmpty() && androidPackages.all { selectedPackages.contains(it) }
          val autoBackgroundChecked = autoBackgroundPackages.isNotEmpty() && autoBackgroundPackages.all { selectedPackages.contains(it) }

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

          LazyColumn {
            item {
              ProtectedAppsSpecialSection(
                selectedPackages = selectedPackages,
                onToggleSelf = { checked ->
                  selectedPackages = if (checked) selectedPackages + context.packageName else selectedPackages - context.packageName
                },
                launcherPackage = launcherPackage,
                launcherChecked = launcherChecked,
                onToggleLauncher = { checked ->
                  launcherPackage?.let { pkg ->
                    selectedPackages = if (checked) selectedPackages + pkg else selectedPackages - pkg
                  }
                },
                keyboardPackage = keyboardPackage,
                keyboardChecked = keyboardChecked,
                onToggleKeyboard = { checked ->
                  keyboardPackage?.let { pkg ->
                    selectedPackages = if (checked) selectedPackages + pkg else selectedPackages - pkg
                  }
                },
                wallpaperPackages = wallpaperPackages,
                wallpaperChecked = wallpaperChecked,
                onToggleWallpaper = { checked ->
                  selectedPackages = if (checked) selectedPackages + wallpaperPackages else selectedPackages - wallpaperPackages
                },
                activeWidgetPackages = activeWidgetPackages,
                widgetsChecked = widgetsChecked,
                onToggleWidgets = { checked ->
                  selectedPackages = if (checked) selectedPackages + activeWidgetPackages else selectedPackages - activeWidgetPackages
                },
                autoBackgroundPackages = autoBackgroundPackages,
                autoBackgroundChecked = autoBackgroundChecked,
                onToggleAutoBackground = { checked ->
                  selectedPackages = if (checked) selectedPackages + autoBackgroundPackages else selectedPackages - autoBackgroundPackages
                },
                androidPackages = androidPackages,
                androidServicesChecked = androidServicesChecked,
                onToggleAndroidServices = { checked ->
                  selectedPackages = if (checked) selectedPackages + androidPackages else selectedPackages - androidPackages.toSet()
                },
                googleAndroidPackages = googleAndroidPackages,
                googleAndroidServicesChecked = googleAndroidServicesChecked,
                onToggleGoogleServices = { checked ->
                  selectedPackages = if (checked) selectedPackages + googleAndroidPackages else selectedPackages - googleAndroidPackages.toSet()
                },
                regexText = regexText,
                onRegexChange = { regexText = it },
              )
            }

            item {
              ProtectedAppsAppList(
                apps = filtered,
                selectedPackages = selectedPackages,
                onToggle = ::togglePackage,
              )
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
            selectedPackages = ProtectionManager.getDefaultProtectedApps(context)
            selectedPackages = applyRegexToSelectedPackages(regexText, allApps, selectedPackages)
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
              ProtectionManager.saveProtectedRegex(context, regexText)
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
