package com.yn.shappky.ui.components

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.yn.shappky.R
import com.yn.shappky.model.AppModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppModel,
    showAppTypeIcons: Boolean,
    onToggle: () -> Unit,
    onKill: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rowBackgroundColor =
        if (app.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val primaryTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
    val rippleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .alpha(if (app.isProtected) 0.4f else 1f)
            .background(rowBackgroundColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = rippleColor),
                onClick = onToggle,
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DrawableIcon(app.appIcon)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                color = primaryTextColor,
                fontSize = 16.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                color = secondaryTextColor,
                fontSize = 12.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showAppTypeIcons) {
                    val icon = when {
                        app.isPersistentApp -> Icons.Outlined.PushPin
                        app.isSystemApp -> Icons.Outlined.Settings
                        else -> Icons.Outlined.Person
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = secondaryTextColor,
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = app.appRam,
                    color = secondaryTextColor,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                )
            }
        }
        if (!app.isProtected && !app.isSelected) {
            IconButton(
                onClick = onKill,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Outlined.Cancel,
                    contentDescription = stringResource(R.string.force_stop),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
    HorizontalDivider()
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
