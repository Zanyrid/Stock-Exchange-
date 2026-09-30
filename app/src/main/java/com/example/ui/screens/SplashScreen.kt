package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BlinkingCursor
import com.example.ui.components.CrtScanlineOverlay
import com.example.ui.components.TerminalButton
import com.example.ui.theme.AmberTerminal
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.PhosphorGreenBright
import com.example.ui.theme.PhosphorGreenDim
import com.example.ui.theme.TerminalBlack
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalDarkBg
import com.example.ui.theme.TerminalSurface
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
  onComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bootLines = remember {
    listOf(
      "COMMODEX-84 BIOS V1.04 (C) 1984 ABSURD CORP.",
      "CPU: Z80-A DUAL VIRTUAL CORE @ 4.00 MHz ... [OK]",
      "CHECKING BASE MEMORY: 64KB PHOSPHOR RAM ... [OK]",
      "SCANNING TANGIBLE/INTANGIBLE COMMODITY BUS...",
      " > GMA.GUA (GEMA GUA) MOUNTED ......... [ONLINE]",
      " > MMP.SNG (MIMPI SIANG) MOUNTED ....... [ONLINE]",
      " > AWN.KUM (AWAN KUMULUS) MOUNTED ..... [ONLINE]",
      " > BSK.PST (BISIKAN PERPUSTAKAAN) ..... [ONLINE]",
      " > ARM.HJN (AROMA HUJAN PERTAMA) ...... [ONLINE]",
      " > KNG.DJV (KENANGAN DEJA VU) ......... [ONLINE]",
      " > NST.KST (NOSTALGIA KASET) .......... [ONLINE]",
      " > WKT.TND (WAKTU TERTUNDA) ........... [ONLINE]",
      "CONFIGURING LOCAL REAL-TIME TICK GENERATOR...",
      "ESTABLISHING PROTOCOL: abs://exchange.absurd.net",
      "ALL SUBSYSTEMS NOMINAL. INITIALIZING WORKSTATION...",
      ">>> PRESS ENTER OR TAP ANYWHERE TO START TRADING <<<"
    )
  }

  val displayedLines = remember { mutableStateListOf<String>() }
  val listState = rememberLazyListState()
  var isFinished by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    for (line in bootLines) {
      delay(110)
      displayedLines.add(line)
      listState.animateScrollToItem(displayedLines.size - 1)
    }
    isFinished = true
    delay(1500)
    onComplete()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TerminalBlack)
      .clickable { onComplete() }
      .padding(16.dp)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Retro ASCII Art Header
      Text(
        text = """
  ____  _   _ ____  ____    _        _   ____  ____  _   _ ____  ____  
 | __ )| | | |  _ \/ ___|  / \      / \ | __ )/ ___|| | | |  _ \|  _ \ 
 |  _ \| | | | |_) \___ \ / _ \    / _ \|  _ \\___ \| | | | |_) | | | |
 | |_) | |_| |  _ < ___) / ___ \  / ___ \ |_) |___) | |_| |  _ <| |_| |
 |____/ \___/|_| \_\____/_/   \_\/_/   \_\____/|____/ \___/|_| \_\____/ 
        """.trimIndent(),
        color = PhosphorGreenBright,
        fontFamily = FontFamily.Monospace,
        fontSize = 7.5.sp,
        lineHeight = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(12.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .background(TerminalDarkBg)
          .border(1.dp, TerminalBorder)
          .padding(12.dp)
      ) {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize()
        ) {
          items(displayedLines) { line ->
            val color = when {
              line.contains("[OK]") -> PhosphorGreen
              line.contains("[ONLINE]") -> PhosphorGreenBright
              line.contains("PRESS ENTER") -> AmberTerminal
              else -> PhosphorGreenDim
            }
            Text(
              text = line,
              color = color,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.5.sp,
              lineHeight = 16.sp
            )
          }

          item {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(top = 4.dp)
            ) {
              Text(
                text = "COMMODEX-84>",
                color = PhosphorGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.width(4.dp))
              BlinkingCursor()
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isFinished) "SISTEM SIAP DIPAKAI" else "MEMUAT DATA BURSA...",
          color = if (isFinished) PhosphorGreen else AmberTerminal,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp
        )

        TerminalButton(
          text = "LEWATI BOOT [ENTER]",
          onClick = onComplete,
          testTag = "skip_boot_button"
        )
      }
    }

    CrtScanlineOverlay()
  }
}
