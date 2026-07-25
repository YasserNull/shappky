package com.yassernull.shappky.ui.components

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun DrawableIcon(
  drawable: Drawable,
  modifier: Modifier = Modifier.size(48.dp),
) {
  AndroidView(
    factory = { context ->
      ImageView(context).apply {
        layoutParams = ViewGroup.LayoutParams(48, 48)
        scaleType = ImageView.ScaleType.FIT_CENTER
      }
    },
    update = { imageView -> imageView.setImageDrawable(drawable) },
    modifier = modifier,
  )
}
