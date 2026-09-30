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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Trader
import com.example.ui.components.TerminalBadge
import com.example.ui.components.TerminalBox
import com.example.ui.components.TerminalHeaderBar
import com.example.ui.theme.AmberTerminal
import com.example.ui.theme.CyanTerminal
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.PhosphorGreenBright
import com.example.ui.theme.PhosphorGreenDark
import com.example.ui.theme.PhosphorGreenDim
import com.example.ui.theme.RedTerminal
import com.example.ui.theme.TerminalBlack
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalBorderGlow
import com.example.ui.theme.TerminalDarkBg
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.YellowTerminal
import java.util.Locale

@Composable
fun LeaderboardScreen(
  traders: List<Trader>,
  modifier: Modifier = Modifier
) {
  var sortMode by remember { mutableStateOf("NET_WORTH") }

  val sortedTraders = remember(traders, sortMode) {
    when (sortMode) {
      "ROI" -> traders.sortedByDescending { it.roiPercent }
      "TRADES" -> traders.sortedByDescending { it.tradeCount }
      else -> traders.sortedByDescending { it.netWorth }
    }
  }

  val userTrader = remember(traders) { traders.find { it.isUser } }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TerminalBlack)
  ) {
    TerminalHeaderBar(
      title = "PAPAN PERINGKAT PIALANG",
      subtitle = "LEADERBOARD SPEKULAN KOMODITAS ABSURD",
      statusText = "RANK: LIVE"
    )

    // User Sticky Standing Card
    if (userTrader != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(TerminalSurface)
          .border(1.dp, PhosphorGreenBright)
          .padding(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            Text(
              text = ">>> STATUS ANDA: PERINGKAT #${userTrader.rank} <<<",
              color = PhosphorGreenBright,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "GELAR: ${userTrader.badge} | TRADE: ${userTrader.tradeCount}",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            )
          }

          Spacer(modifier = Modifier.weight(1f))

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = String.format(Locale.US, "%,.2f ABS", userTrader.netWorth),
              color = PhosphorGreenBright,
              fontFamily = FontFamily.Monospace,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            val roiColor = if (userTrader.roiPercent >= 0) PhosphorGreen else RedTerminal
            val roiSign = if (userTrader.roiPercent >= 0) "+" else ""
            Text(
              text = String.format(Locale.US, "ROI: %s%.1f%%", roiSign, userTrader.roiPercent),
              color = roiColor,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    // Sort buttons
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(TerminalDarkBg)
        .border(1.dp, TerminalBorder)
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Text(
        text = "URUTKAN:",
        color = PhosphorGreenDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        modifier = Modifier.align(Alignment.CenterVertically)
      )

      SortChip(
        label = "KEKAYAAN",
        active = sortMode == "NET_WORTH",
        onClick = { sortMode = "NET_WORTH" }
      )
      SortChip(
        label = "ROI %",
        active = sortMode == "ROI",
        onClick = { sortMode = "ROI" }
      )
      SortChip(
        label = "VOLUME TRADE",
        active = sortMode == "TRADES",
        onClick = { sortMode = "TRADES" }
      )
    }

    // Trader list
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      items(sortedTraders) { trader ->
        TraderRow(trader = trader)
      }

      item {
        Spacer(modifier = Modifier.height(80.dp))
      }
    }
  }
}

@Composable
private fun SortChip(label: String, active: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .background(if (active) PhosphorGreenDim.copy(alpha = 0.4f) else TerminalSurface)
      .border(1.dp, if (active) PhosphorGreen else TerminalBorder)
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Text(
      text = if (active) "[$label]" else label,
      color = if (active) PhosphorGreenBright else PhosphorGreenDim,
      fontFamily = FontFamily.Monospace,
      fontSize = 10.sp,
      fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
    )
  }
}

@Composable
private fun TraderRow(trader: Trader) {
  val isTop3 = trader.rank <= 3
  val rankBadgeColor = when (trader.rank) {
    1 -> YellowTerminal
    2 -> CyanTerminal
    3 -> AmberTerminal
    else -> PhosphorGreenDim
  }

  val borderColor = if (trader.isUser) PhosphorGreenBright else if (isTop3) rankBadgeColor.copy(alpha = 0.7f) else TerminalBorder
  val bgColor = if (trader.isUser) TerminalSurface else TerminalDarkBg

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(bgColor)
      .border(1.dp, borderColor)
      .padding(8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      // Rank marker
      Box(
        modifier = Modifier
          .width(36.dp)
          .border(1.dp, rankBadgeColor)
          .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "#${trader.rank}",
          color = rankBadgeColor,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Trader info
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = trader.username,
            color = if (trader.isUser) PhosphorGreenBright else PhosphorGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          if (trader.isUser) {
            Spacer(modifier = Modifier.width(4.dp))
            TerminalBadge(text = "ANDA", color = PhosphorGreenBright)
          }
        }
        Text(
          text = trader.badge,
          color = PhosphorGreenDim,
          fontFamily = FontFamily.Monospace,
          fontSize = 9.5.sp
        )
      }

      // Net Worth & ROI
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = String.format(Locale.US, "%,.0f ABS", trader.netWorth),
          color = PhosphorGreenBright,
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
        val roiSign = if (trader.roiPercent >= 0) "+" else ""
        val roiColor = if (trader.roiPercent >= 0) PhosphorGreen else RedTerminal
        Text(
          text = String.format(Locale.US, "ROI: %s%.1f%%", roiSign, trader.roiPercent),
          color = roiColor,
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp
        )
      }
    }
  }
}
