package com.yassernull.shappky.ui.dialogs

import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.yassernull.shappky.ui.components.ProtectedAppsSearchBar
import com.yassernull.shappky.ui.components.ProtectedAppsSpecialSection
import com.yassernull.shappky.ui.components.protectedAppsAppList
import com.yassernull.shappky.utils.collectActiveWidgetPackages
import com.yassernull.shappky.utils.collectCurrentWallpaperPackages
import com.yassernull.shappky.utils.getAndroidPackages
import com.yassernull.shappky.utils.getKeyboardPackage
import com.yassernull.shappky.utils.getLauncherPackage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun ProtectedAppsDialog(
  loadAllApps: ((List<AppModel>) -> Unit) -> Unit,
  onDismiss: () -> Unit,
  onSave: (Set<String>) -> Unit,
) {
  val context = LocalContext.current
  val pm = context.packageManager
  val scope = rememberCoroutineScope()

  var query by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(true) }
  val allApps = remember { mutableStateListOf<AppModel>() }
  var selectedPackages by remember { mutableStateOf(ProtectionManager.getProtectedApps(context)) }
  var exemptions by remember { mutableStateOf(ProtectionManager.getProtectedAppsExemptions(context)) }

  val launcherPackage = remember { pm.getLauncherPackage() }
  val keyboardPackage = remember { context.getKeyboardPackage() }

  var activeWidgetPackages by remember { mutableStateOf(emptySet<String>()) }
  var wallpaperPackages by remember { mutableStateOf(emptySet<String>()) }
  val groupNames = listOf("launcher", "keyboard", "wallpaper", "persistent", "widgets", "android", "google")
  var groupToggles by remember {
    mutableStateOf(groupNames.associateWith { ProtectionManager.getGroupEnabled(context, it) })
  }
  var groupMembers by remember {
    mutableStateOf(groupNames.associateWith { ProtectionManager.getGroupMembers(context, it) })
  }
  var regexText by remember { mutableStateOf("") }
  var previousRegexText by remember { mutableStateOf("") }
  var showUserApps by remember { mutableStateOf(true) }
  var showSystemApps by remember { mutableStateOf(true) }
  var showPersistentApps by remember { mutableStateOf(true) }
  var isMenuExpanded by remember { mutableStateOf(false) }

  val androidPackages = remember(allApps.size) { getAndroidPackages(allApps) }

  fun matchesRegexText(regexText: String, pkg: String): Boolean {
    if (regexText.isBlank()) return false
    return regexText.split("|").any { pattern ->
      try {
        pattern.replace(".", "\\.").replace("*", ".*").toRegex().matches(pkg)
      } catch (_: Exception) {
        (pattern.endsWith(".*") && pkg.startsWith(pattern.removeSuffix(".*"))) || pkg == pattern
      }
    }
  }

  fun matchesCurrentRegex(pkg: String): Boolean = matchesRegexText(regexText, pkg)

  fun isEffectivelyProtected(pkg: String): Boolean = selectedPackages.contains(pkg) ||
    ProtectionManager.getEnabledGroupProtectedPackages(context).contains(pkg) ||
    (matchesCurrentRegex(pkg) && !exemptions.contains(pkg))

  fun protect(packages: Set<String>) {
    selectedPackages = selectedPackages + packages
    exemptions = exemptions - packages
  }

  fun unprotect(packages: Set<String>) {
    selectedPackages = selectedPackages - packages
    exemptions = exemptions + packages.filter { matchesCurrentRegex(it) }
  }

  fun togglePackage(pkg: String) {
    if (isEffectivelyProtected(pkg)) unprotect(setOf(pkg)) else protect(setOf(pkg))
  }

  fun toggleGroup(group: String, packages: Set<String>, checked: Boolean) {
    val storedMembers = groupMembers[group] ?: emptySet()
    val members = if (checked) storedMembers + packages else storedMembers.ifEmpty { packages }
    if (checked) {
      protect(members)
    } else {
      unprotect(members)
    }
    groupMembers = groupMembers + (group to if (checked) members else emptySet<String>())
    groupToggles = groupToggles + (group to checked)
  }

  LaunchedEffect(Unit) {
    regexText = ProtectionManager.getProtectedRegex(context)
    loadAllApps { result ->
      allApps.clear()
      allApps.addAll(result)
      isLoading = false
    }

    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val executor = Executors.newSingleThreadExecutor()
    val shellManager = ShellManager(context, handler, executor)
    activeWidgetPackages = context.collectActiveWidgetPackages(shellManager)
    wallpaperPackages = context.collectCurrentWallpaperPackages(shellManager)

    if (wallpaperPackages.isNotEmpty() && (groupToggles["wallpaper"] ?: true)) {
      selectedPackages = selectedPackages + wallpaperPackages
    }
    if (activeWidgetPackages.isNotEmpty() && (groupToggles["widgets"] ?: true)) {
      selectedPackages = selectedPackages + activeWidgetPackages
    }
  }

  LaunchedEffect(allApps.size) {
    if (allApps.isNotEmpty()) {
      val persistentPackages = allApps.filter { it.isPersistentApp }.map { it.packageName }
      if (persistentPackages.isNotEmpty() && (groupToggles["persistent"] ?: true)) {
        selectedPackages = selectedPackages + persistentPackages
      }
    }
  }

  val hasLoggedDiagnostics = remember { mutableStateOf(false) }
  LaunchedEffect(allApps.size, activeWidgetPackages) {
    if (!hasLoggedDiagnostics.value && allApps.isNotEmpty()) {
      hasLoggedDiagnostics.value = true
      val checkedApps = allApps.filter { isEffectivelyProtected(it.packageName) }
      Log.d("ProtectedAppsDialog", "Diagnostic: ${checkedApps.size} checked apps of ${allApps.size} total")
      for (app in checkedApps) {
        val reasons = mutableListOf<String>()
        if (selectedPackages.contains(app.packageName)) reasons.add("saved")
        if (matchesCurrentRegex(app.packageName)) reasons.add("regex")
        if (app.isPersistentApp) reasons.add("persistent")
        if (launcherPackage == app.packageName) reasons.add("launcher")
        if (keyboardPackage == app.packageName) reasons.add("keyboard")
        if (wallpaperPackages.contains(app.packageName)) reasons.add("wallpaper")
        if (activeWidgetPackages.contains(app.packageName)) reasons.add("widgets")
        if (androidPackages.contains(app.packageName)) reasons.add("androidServices")
        if (app.packageName.startsWith("com.google.android")) reasons.add("googleServices")
        Log.d("ProtectedAppsDialog", "Checked: ${app.appName} (${app.packageName}) -> ${reasons.joinToString(", ")}")
      }
    }
  }

  LaunchedEffect(regexText) {
    if (regexText.isBlank() && previousRegexText.isNotBlank() && allApps.isNotEmpty()) {
      val previousMatched = allApps.mapNotNull { app ->
        if (matchesRegexText(previousRegexText, app.packageName)) app.packageName else null
      }.toSet()
      if (previousMatched.isNotEmpty()) {
        selectedPackages = selectedPackages - previousMatched
        exemptions = exemptions - previousMatched
      }
    }
    previousRegexText = regexText
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
          val persistentPackages = allApps.filter { it.isPersistentApp }.map { it.packageName }

          val effectiveProtected = allApps.mapNotNull { app ->
            if (isEffectivelyProtected(app.packageName)) app.packageName else null
          }.toSet()

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
                selfChecked = isEffectivelyProtected(context.packageName),
                onToggleSelf = { checked ->
                  if (checked) protect(setOf(context.packageName)) else unprotect(setOf(context.packageName))
                },
                launcherPackage = launcherPackage,
                launcherChecked = launcherPackage != null && isEffectivelyProtected(launcherPackage),
                onToggleLauncher = { checked ->
                  launcherPackage?.let { toggleGroup("launcher", setOf(it), checked) }
                },
                keyboardPackage = keyboardPackage,
                keyboardChecked = keyboardPackage != null && isEffectivelyProtected(keyboardPackage),
                onToggleKeyboard = { checked ->
                  keyboardPackage?.let { toggleGroup("keyboard", setOf(it), checked) }
                },
                persistentPackages = persistentPackages,
                persistentChecked = if (persistentPackages.isNotEmpty()) persistentPackages.all { isEffectivelyProtected(it) } else (groupToggles["persistent"] ?: true),
                onTogglePersistent = { checked ->
                  toggleGroup("persistent", persistentPackages.toSet(), checked)
                },
                wallpaperPackages = wallpaperPackages,
                wallpaperChecked = if (wallpaperPackages.isNotEmpty()) wallpaperPackages.all { isEffectivelyProtected(it) } else (groupToggles["wallpaper"] ?: true),
                onToggleWallpaper = { checked ->
                  toggleGroup("wallpaper", wallpaperPackages, checked)
                },
                activeWidgetPackages = activeWidgetPackages,
                widgetsChecked = if (activeWidgetPackages.isNotEmpty()) activeWidgetPackages.all { isEffectivelyProtected(it) } else (groupToggles["widgets"] ?: true),
                onToggleWidgets = { checked ->
                  scope.launch {
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    val executor = Executors.newSingleThreadExecutor()
                    val packages = context.collectActiveWidgetPackages(ShellManager(context, handler, executor))
                    activeWidgetPackages = packages
                    toggleGroup("widgets", packages, checked)
                  }
                },
                androidPackages = androidPackages,
                androidServicesChecked = androidPackages.isNotEmpty() && androidPackages.all { isEffectivelyProtected(it) },
                onToggleAndroidServices = { checked ->
                  toggleGroup("android", androidPackages.toSet(), checked)
                },
                googleAndroidPackages = googleAndroidPackages,
                googleAndroidServicesChecked = googleAndroidPackages.isNotEmpty() && googleAndroidPackages.all { isEffectivelyProtected(it) },
                onToggleGoogleServices = { checked ->
                  toggleGroup("google", googleAndroidPackages.toSet(), checked)
                },
                regexText = regexText,
                onRegexChange = { regexText = it },
              )
            }

            protectedAppsAppList(
              apps = filtered,
              selectedPackages = effectiveProtected,
              onToggle = ::togglePackage,
            )
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
            exemptions = emptySet()
            groupToggles = groupNames.associateWith { false }
            groupMembers = groupNames.associateWith { emptySet<String>() }
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
              ProtectionManager.saveProtectedAppsExemptions(context, exemptions)
              groupNames.forEach { group ->
                ProtectionManager.saveGroupState(
                  context,
                  group,
                  groupToggles[group] ?: false,
                  groupMembers[group] ?: emptySet(),
                )
              }
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
