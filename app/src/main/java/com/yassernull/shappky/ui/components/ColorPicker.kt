package com.yassernull.shappky.ui.components

import android.content.Context
import android.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yassernull.shappky.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun ColorWheel(
  hue: Float,
  saturation: Float,
  onColorChanged: (hue: Float, saturation: Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = remember {
    listOf(
      ComposeColor.Red,
      ComposeColor.Yellow,
      ComposeColor.Green,
      ComposeColor.Cyan,
      ComposeColor.Blue,
      ComposeColor.Magenta,
      ComposeColor.Red,
    )
  }

  BoxWithConstraints(
    modifier = modifier
      .aspectRatio(1f)
      .fillMaxWidth(),
  ) {
    val sizePx = constraints.maxWidth
    val radius = sizePx / 2f

    val updateColor = { pos: Offset ->
      val dx = pos.x - radius
      val dy = pos.y - radius
      val distance = sqrt(dx * dx + dy * dy)
      val sat = (distance / radius).coerceIn(0f, 1f)

      var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
      if (angle < 0) {
        angle += 360f
      }
      onColorChanged(angle, sat)
    }

    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(radius) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              val change = event.changes.firstOrNull() ?: continue
              if (change.pressed) {
                updateColor(change.position)
                change.consume()
              }
            }
          }
        },
    ) {
      drawCircle(
        brush = Brush.sweepGradient(colors),
        radius = radius,
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(ComposeColor.White, ComposeColor.Transparent),
          radius = radius,
        ),
        radius = radius,
      )

      val angleRad = Math.toRadians(hue.toDouble())
      val distance = saturation * radius
      val indicatorX = radius + distance * cos(angleRad).toFloat()
      val indicatorY = radius + distance * sin(angleRad).toFloat()

      drawCircle(
        color = ComposeColor.Black,
        radius = 8.dp.toPx(),
        center = Offset(indicatorX, indicatorY),
        style = Stroke(width = 2.dp.toPx()),
      )
      drawCircle(
        color = ComposeColor.White,
        radius = 6.dp.toPx(),
        center = Offset(indicatorX, indicatorY),
      )
    }
  }
}

@Composable
fun ColorPickerDialog(
  initialColor: Int,
  showAlpha: Boolean = true,
  onDismiss: () -> Unit,
  onColorSelected: (Int) -> Unit,
) {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE) }
  val theme = remember { prefs.getString("appTheme", "dark") ?: "dark" }

  var hue by remember {
    val hsv = FloatArray(3)
    Color.colorToHSV(initialColor, hsv)
    mutableStateOf(hsv[0])
  }
  var saturation by remember {
    val hsv = FloatArray(3)
    Color.colorToHSV(initialColor, hsv)
    mutableStateOf(hsv[1])
  }
  var value by remember {
    val hsv = FloatArray(3)
    Color.colorToHSV(initialColor, hsv)
    mutableStateOf(hsv[2])
  }
  var alpha by remember {
    mutableStateOf(Color.alpha(initialColor) / 255f)
  }

  val currentColor = remember(hue, saturation, value, alpha) {
    Color.HSVToColor((alpha * 255).toInt(), floatArrayOf(hue, saturation, value))
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.widget_select_color)) },
    containerColor = if (theme == "black" || theme == "dark") ComposeColor(0xFF121212) else MaterialTheme.colorScheme.surface,
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(ComposeColor(currentColor))
            .border(2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
        )
        Spacer(Modifier.height(16.dp))

        val hexString = String.format("#%08X", currentColor)
        Text(
          text = hexString,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))

        ColorWheel(
          hue = hue,
          saturation = saturation,
          onColorChanged = { newHue, newSat ->
            hue = newHue
            saturation = newSat
          },
          modifier = Modifier
            .size(180.dp)
            .padding(bottom = 12.dp),
        )

        Text(
          text = stringResource(R.string.widget_brightness),
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.align(Alignment.Start),
        )
        Slider(
          value = value,
          onValueChange = { value = it },
          valueRange = 0f..1f,
          modifier = Modifier.fillMaxWidth(),
        )

        if (showAlpha) {
          Text(
            text = stringResource(R.string.widget_opacity),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Start),
          )
          Slider(
            value = alpha,
            onValueChange = { alpha = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onColorSelected(currentColor) }) {
        Text(stringResource(R.string.widget_select))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}
