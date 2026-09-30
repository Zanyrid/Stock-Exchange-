package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.TerminalBlack
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalBorderGlow
import com.example.ui.theme.TerminalDarkBg
import com.example.ui.theme.TerminalSurface
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CrtScanlineOverlay(
  modifier: Modifier = Modifier,
  enabled: Boolean = true
) {
  if (!enabled) return

  val infiniteTransition = rememberInfiniteTransition(label = "crt_scanline")
  val scanOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 20f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000),
      repeatMode = RepeatMode.Restart
    ),
    label = "scan_offset"
  )

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .alpha(0.35f)
  ) {
    val lineHeight = 3.dp.toPx()
    val gap = 5.dp.toPx()
    var y = scanOffset % gap
    while (y < size.height) {
      drawLine(
        color = Color(0xFF030804),
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = lineHeight
      )
      y += gap
    }
  }
}

@Composable
fun BlinkingCursor(
  modifier: Modifier = Modifier,
  symbol: String = "█",
  color: Color = PhosphorGreen
) {
  val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
  val alpha by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(500),
      repeatMode = RepeatMode.Reverse
    ),
    label = "cursor_alpha"
  )

  Text(
    text = symbol,
    color = color,
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    modifier = modifier.alpha(alpha)
  )
}

@Composable
fun TerminalBox(
  modifier: Modifier = Modifier,
  title: String? = null,
  borderColor: Color = TerminalBorderGlow,
  backgroundColor: Color = TerminalDarkBg,
  glow: Boolean = false,
  content: @Composable BoxScope.() -> Unit
) {
  Column(modifier = modifier) {
    if (title != null) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .background(TerminalSurface)
          .border(1.dp, borderColor)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "+--[ $title ]",
          color = PhosphorGreenBright,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = "--+",
          color = borderColor,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(backgroundColor)
        .border(1.dp, borderColor)
        .padding(10.dp)
    ) {
      content()
    }
  }
}

@Composable
fun TerminalHeaderBar(
  title: String = "BURSA KOMODITAS ABSURD",
  subtitle: String = "COMMODEX-84 TRADING OS",
  statusText: String = "FEED: LIVE",
  alertsCount: Int = 0,
  modifier: Modifier = Modifier
) {
  var currentTime by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    while (true) {
      currentTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
      delay(1000)
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(TerminalBlack)
      .border(1.dp, TerminalBorder)
  ) {
    // Top status line
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(TerminalSurface)
        .padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "SYS:OK",
        color = PhosphorGreenDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "BAUD:9600",
        color = PhosphorGreenDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "[$statusText]",
        color = PhosphorGreen,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.weight(1f))

      if (alertsCount > 0) {
        Text(
          text = "!ALERTS:$alertsCount",
          color = AmberTerminal,
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
      }

      Text(
        text = currentTime,
        color = PhosphorGreenBright,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp
      )
    }

    // Main header branding
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "> $title",
            color = PhosphorGreenBright,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.width(4.dp))
          BlinkingCursor()
        }
        Text(
          text = subtitle,
          color = PhosphorGreenDim,
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp
        )
      }
    }
  }
}

@Composable
fun TerminalButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  color: Color = PhosphorGreen,
  backgroundColor: Color = TerminalSurface,
  borderColor: Color = PhosphorGreen,
  enabled: Boolean = true,
  testTag: String = "terminal_button"
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .testTag(testTag)
      .alpha(if (enabled) 1f else 0.5f)
      .background(backgroundColor)
      .border(1.dp, if (enabled) borderColor else TerminalBorder)
      .clickable(enabled = enabled, onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Text(
      text = "[ $text ]",
      color = if (enabled) color else PhosphorGreenDim,
      fontFamily = FontFamily.Monospace,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 0.5.sp
    )
  }
}

@Composable
fun TerminalBadge(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = PhosphorGreen,
  borderColor: Color = color
) {
  Box(
    modifier = modifier
      .background(TerminalDarkBg)
      .border(1.dp, borderColor)
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = text,
      color = color,
      fontFamily = FontFamily.Monospace,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
