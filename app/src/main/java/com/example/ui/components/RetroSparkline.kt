package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberTerminal
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.PhosphorGreenBright
import com.example.ui.theme.PhosphorGreenDark
import com.example.ui.theme.PhosphorGreenDim
import com.example.ui.theme.RedTerminal
import com.example.ui.theme.TerminalBlack
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalDarkBg
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RetroMiniSparkline(
  data: List<Double>,
  modifier: Modifier = Modifier,
  isPositive: Boolean = true,
  strokeWidth: Dp = 1.8.dp
) {
  if (data.size < 2) return

  val lineColor = if (isPositive) PhosphorGreen else AmberTerminal
  val glowColor = if (isPositive) PhosphorGreen.copy(alpha = 0.2f) else AmberTerminal.copy(alpha = 0.2f)

  Canvas(modifier = modifier) {
    val minVal = data.minOrNull() ?: 0.0
    val maxVal = data.maxOrNull() ?: 1.0
    val range = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal

    val width = size.width
    val height = size.height
    val stepX = width / (data.size - 1)

    val path = Path()
    val fillPath = Path()

    data.forEachIndexed { i, value ->
      val x = i * stepX
      val normalized = ((value - minVal) / range).toFloat()
      val y = height - (normalized * (height - 8f)) - 4f

      if (i == 0) {
        path.moveTo(x, y)
        fillPath.moveTo(x, height)
        fillPath.lineTo(x, y)
      } else {
        path.lineTo(x, y)
        fillPath.lineTo(x, y)
      }
    }

    fillPath.lineTo(width, height)
    fillPath.close()

    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(glowColor, Color.Transparent),
        startY = 0f,
        endY = height
      )
    )

    drawPath(
      path = path,
      color = lineColor,
      style = Stroke(width = strokeWidth.toPx())
    )
  }
}

@Composable
fun RetroDetailedChart(
  data: List<Double>,
  modifier: Modifier = Modifier,
  unit: String = "ABS",
  height: Dp = 180.dp
) {
  if (data.isEmpty()) return

  var selectedIndex by remember { mutableStateOf<Int?>(null) }
  val minVal = data.minOrNull() ?: 0.0
  val maxVal = data.maxOrNull() ?: 1.0
  val range = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal

  val isUp = (data.lastOrNull() ?: 0.0) >= (data.firstOrNull() ?: 0.0)
  val chartColor = if (isUp) PhosphorGreenBright else AmberTerminal

  val displayedPrice = if (selectedIndex != null && selectedIndex!! in data.indices) {
    data[selectedIndex!!]
  } else {
    data.last()
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(TerminalDarkBg)
      .border(1.dp, TerminalBorder)
      .padding(8.dp)
  ) {
    // Chart Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = if (selectedIndex != null) "INSPECT POINT [T-$selectedIndex]:" else "HARGA TERAKHIR:",
        color = PhosphorGreenDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = String.format(Locale.US, "%,.2f %s", displayedPrice, unit),
        color = chartColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = "VOL: 30 TICKS",
        color = PhosphorGreenDark,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Canvas Plotting Area
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(height)
    ) {
      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(data) {
            detectTapGestures { offset ->
              val stepX = size.width / (data.size - 1)
              val idx = (offset.x / stepX).roundToInt().coerceIn(0, data.lastIndex)
              selectedIndex = idx
            }
          }
          .pointerInput(data) {
            detectDragGestures(
              onDragStart = { offset ->
                val stepX = size.width / (data.size - 1)
                val idx = (offset.x / stepX).roundToInt().coerceIn(0, data.lastIndex)
                selectedIndex = idx
              },
              onDragEnd = { selectedIndex = null },
              onDragCancel = { selectedIndex = null },
              onDrag = { change, _ ->
                val stepX = size.width / (data.size - 1)
                val idx = (change.position.x / stepX).roundToInt().coerceIn(0, data.lastIndex)
                selectedIndex = idx
              }
            )
          }
      ) {
        val w = size.width
        val h = size.height

        // Background retro grid lines (3 horizontal lines)
        val gridLines = 4
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        for (i in 0..gridLines) {
          val y = (h / gridLines) * i
          drawLine(
            color = PhosphorGreenDark.copy(alpha = 0.5f),
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f,
            pathEffect = dashEffect
          )
        }

        // Vertical tick lines
        val vLines = 5
        for (i in 0..vLines) {
          val x = (w / vLines) * i
          drawLine(
            color = PhosphorGreenDark.copy(alpha = 0.3f),
            start = Offset(x, 0f),
            end = Offset(x, h),
            strokeWidth = 1f,
            pathEffect = dashEffect
          )
        }

        if (data.size >= 2) {
          val stepX = w / (data.size - 1)
          val path = Path()
          val fillPath = Path()

          data.forEachIndexed { i, value ->
            val x = i * stepX
            val normalized = ((value - minVal) / range).toFloat()
            val y = h - (normalized * (h - 24f)) - 12f

            if (i == 0) {
              path.moveTo(x, y)
              fillPath.moveTo(x, h)
              fillPath.lineTo(x, y)
            } else {
              path.lineTo(x, y)
              fillPath.lineTo(x, y)
            }
          }

          fillPath.lineTo(w, h)
          fillPath.close()

          // Draw fill gradient below line
          drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
              colors = listOf(chartColor.copy(alpha = 0.25f), Color.Transparent),
              startY = 0f,
              endY = h
            )
          )

          // Draw main phosphor line
          drawPath(
            path = path,
            color = chartColor,
            style = Stroke(width = 2.5f)
          )

          // Draw data points
          data.forEachIndexed { i, value ->
            val x = i * stepX
            val normalized = ((value - minVal) / range).toFloat()
            val y = h - (normalized * (h - 24f)) - 12f

            if (i == data.lastIndex || i == selectedIndex) {
              drawCircle(
                color = chartColor,
                radius = 5f,
                center = Offset(x, y)
              )
              drawCircle(
                color = TerminalBlack,
                radius = 2.5f,
                center = Offset(x, y)
              )
            }
          }

          // Draw crosshair if point is selected
          selectedIndex?.let { idx ->
            if (idx in data.indices) {
              val x = idx * stepX
              val value = data[idx]
              val normalized = ((value - minVal) / range).toFloat()
              val y = h - (normalized * (h - 24f)) - 12f

              // Vertical crosshair
              drawLine(
                color = chartColor.copy(alpha = 0.7f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
              )
              // Horizontal crosshair
              drawLine(
                color = chartColor.copy(alpha = 0.7f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
              )
            }
          }
        }
      }
    }

    // Chart footer min/max values
    Spacer(modifier = Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "MIN: %,.2f".format(minVal),
        color = PhosphorGreenDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = "MAX: %,.2f".format(maxVal),
        color = PhosphorGreenBright,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp
      )
    }
  }
}
