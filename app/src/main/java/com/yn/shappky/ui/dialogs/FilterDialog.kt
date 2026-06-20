package com.yn.shappky.ui.dialogs

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yn.shappky.R
import com.yn.shappky.model.AppModel

@Composable
fun FilterDialog(
    hiddenApps: Set<String>,
    loadAllApps: ((List<AppModel>) -> Unit) -> Unit,
    onSaveHiddenApps: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    var query by mutableStateOf("")
    var isLoading by mutableStateOf(true)
    val allApps = mutableStateListOf<AppModel>()
    var selectedPackages by mutableStateOf(hiddenApps)

    LaunchedEffect(Unit) {
        loadAllApps { result ->
            allApps.clear()
            allApps.addAll(result)
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_dialog_title)) },
        text = {
            Column(modifier = Modifier.height(460.dp)) {
                SearchField(query = query, onQueryChange = { query = it })
                Spacer(Modifier.height(8.dp))
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filtered = allApps.filter {
                        it.appName.contains(query, ignoreCase = true)
                    }
                    LazyColumn {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPackages =
                                            if (selectedPackages.contains(app.packageName)) {
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
                                Text(
                                    text = app.appName,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Checkbox(
                                    checked = selectedPackages.contains(app.packageName),
                                    onCheckedChange = { checked ->
                                        selectedPackages =
                                            if (checked) {
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
            TextButton(
                onClick = {
                    onSaveHiddenApps(selectedPackages)
                    onSaved()
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        stringResource(R.string.search_apps),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
                inner()
            },
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
