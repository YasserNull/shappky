package com.yassernull.shappky.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val memoryAltIcon: ImageVector by lazy {
  ImageVector.Builder(
    name = "memory_alt",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  )
    .apply {
      path(
        fill = SolidColor(Color.Black),
        fillAlpha = 1f,
        stroke = null,
        strokeAlpha = 1f,
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Butt,
        strokeLineJoin = StrokeJoin.Bevel,
        strokeLineMiter = 1f,
        pathFillType = PathFillType.Companion.NonZero,
      ) {
        moveTo(6f, 15f)
        horizontalLineTo(8f)
        verticalLineTo(9f)
        horizontalLineTo(6f)
        verticalLineToRelative(6f)
        close()
        moveToRelative(5f, 0f)
        horizontalLineToRelative(2f)
        verticalLineTo(9f)
        horizontalLineTo(11f)
        verticalLineToRelative(6f)
        close()
        moveToRelative(5f, 0f)
        horizontalLineToRelative(2f)
        verticalLineTo(9f)
        horizontalLineTo(16f)
        verticalLineToRelative(6f)
        close()
        moveTo(4f, 17f)
        horizontalLineTo(20f)
        verticalLineTo(7f)
        horizontalLineTo(4f)
        verticalLineTo(17f)
        close()
        moveToRelative(0f, 0f)
        verticalLineTo(7f)
        verticalLineTo(17f)
        close()
        moveToRelative(1f, 4f)
        verticalLineTo(19f)
        horizontalLineTo(4f)
        quadTo(3.18f, 19f, 2.59f, 18.41f)
        reflectiveQuadTo(2f, 17f)
        verticalLineTo(7f)
        quadTo(2f, 6.18f, 2.59f, 5.59f)
        reflectiveQuadTo(4f, 5f)
        horizontalLineTo(5f)
        verticalLineTo(3f)
        horizontalLineTo(7f)
        verticalLineTo(5f)
        horizontalLineToRelative(4f)
        verticalLineTo(3f)
        horizontalLineToRelative(2f)
        verticalLineTo(5f)
        horizontalLineToRelative(4f)
        verticalLineTo(3f)
        horizontalLineToRelative(2f)
        verticalLineTo(5f)
        horizontalLineToRelative(1f)
        quadToRelative(0.83f, 0f, 1.41f, 0.59f)
        quadTo(22f, 6.18f, 22f, 7f)
        verticalLineTo(17f)
        quadToRelative(0f, 0.82f, -0.59f, 1.41f)
        reflectiveQuadTo(20f, 19f)
        horizontalLineTo(19f)
        verticalLineToRelative(2f)
        horizontalLineTo(17f)
        verticalLineTo(19f)
        horizontalLineTo(13f)
        verticalLineToRelative(2f)
        horizontalLineTo(11f)
        verticalLineTo(19f)
        horizontalLineTo(7f)
        verticalLineToRelative(2f)
        horizontalLineTo(5f)
        close()
      }
    }
    .build()
}
