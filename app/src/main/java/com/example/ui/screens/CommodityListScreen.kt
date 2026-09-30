package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.Commodity
import com.example.ui.components.RetroMiniSparkline
import com.example.ui.components.TerminalBadge
import com.example.ui.components.TerminalBox
import com.example.ui.components.TerminalButton
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
import com.example.ui.theme.TerminalDarkBg
import com.example.ui.theme.TerminalSurface
import java.util.Locale

@Composable
fun CommodityListScreen(
  commodities: List<Commodity>,
  alertsCount: Int,
  onCommodityClick: (Commodity) -> Unit,
  onQuickTradeClick: (Commodity) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedCategory by remember { mutableStateOf("SEMUA") }
  val categories = remember {
    listOf("SEMUA", "AKUSTIK GHAIB", "SUBCONSCIOUS", "METEOROLOGI", "DIMENSI WAKTU", "NOSTALGIA")
  }

  val filteredCommodities = remember(commodities, selectedCategory) {
    if (selectedCategory == "SEMUA") commodities
    else commodities.filter { it.category.equals(selectedCategory, ignoreCase = true) }
  }

  val topGainer = remember(commodities) {
    commodities.maxByOrNull { it.change24hPercent }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TerminalBlack)
  ) {
    TerminalHeaderBar(
      title = "PASAR KOMODITAS TAK BERWUJUD",
      subtitle = "PAPAN KURS EKSISTENSIAL 1984",
      statusText = "FEED: LIVE",
      alertsCount = alertsCount
    )

    // Market summary ticker
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(TerminalDarkBg)
        .border(1.dp, TerminalBorder)
        .padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "TOP GAINER:",
        color = PhosphorGreenDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp
      )
      Spacer(modifier = Modifier.width(6.dp))
      if (topGainer != null) {
        Text(
          text = "${topGainer.symbol} (+${String.format(Locale.US, "%.1f", topGainer.change24hPercent)}%)",
          color = PhosphorGreenBright,
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = "SENTIMEN: SURREAL",
        color = AmberTerminal,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
      )
    }

    // Category filter chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      categories.forEach { cat ->
        val isSelected = selectedCategory == cat
        Box(
          modifier = Modifier
            .background(if (isSelected) PhosphorGreenDim.copy(alpha = 0.4f) else TerminalSurface)
            .border(1.dp, if (isSelected) PhosphorGreen else TerminalBorder)
            .clickable { selectedCategory = cat }
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = if (isSelected) "[*$cat*]" else " $cat ",
            color = if (isSelected) PhosphorGreenBright else PhosphorGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }

    // Commodity List
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(
        items = filteredCommodities,
        key = { it.id }
      ) { item ->
        CommodityListItem(
          commodity = item,
          onClick = { onCommodityClick(item) },
          onTrade = { onQuickTradeClick(item) }
        )
      }

      item {
        Spacer(modifier = Modifier.height(16.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "--- AKHIR DARI DAFTAR KOMODITAS ---",
            color = PhosphorGreenDark,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
          )
        }
        Spacer(modifier = Modifier.height(80.dp)) // padding for bottom bar
      }
    }
  }
}

@Composable
fun CommodityListItem(
  commodity: Commodity,
  onClick: () -> Unit,
  onTrade: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isPositive = commodity.isPositive
  val changeColor = if (isPositive) PhosphorGreenBright else RedTerminal
  val changeSign = if (isPositive) "+" else ""

  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(TerminalDarkBg)
      .border(1.dp, TerminalBorder)
      .clickable(onClick = onClick)
      .padding(10.dp)
      .testTag("commodity_item_${commodity.symbol}")
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Top row: Symbol, Name, Category
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = commodity.symbol,
          color = PhosphorGreenBright,
          fontFamily = FontFamily.Monospace,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = commodity.name,
          color = PhosphorGreen,
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp,
          modifier = Modifier.weight(1f)
        )
        TerminalBadge(
          text = commodity.category,
          color = CyanTerminal,
          borderColor = CyanTerminal.copy(alpha = 0.5f)
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Middle row: Price + Sparkline + 24h Change
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.width(115.dp)) {
          Text(
            text = String.format(Locale.US, "%,.2f", commodity.currentPrice),
            color = PhosphorGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "ABS / ${commodity.unit}",
            color = PhosphorGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp
          )
        }

        // Mini Sparkline
        RetroMiniSparkline(
          data = commodity.priceHistory,
          isPositive = isPositive,
          modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .padding(horizontal = 8.dp)
        )

        // Percentage & Quick Action
        Column(
          horizontalAlignment = Alignment.End,
          modifier = Modifier.width(80.dp)
        ) {
          Text(
            text = String.format(Locale.US, "%s%.2f%%", changeSign, commodity.change24hPercent),
            color = changeColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "24H: ${String.format(Locale.US, "%s%,.1f", changeSign, commodity.change24h)}",
            color = PhosphorGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Bottom row: Stats and trade button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "VOL: %,d | VOLATIL: %s".format(commodity.volume24h, commodity.volatility.name),
          color = PhosphorGreenDark,
          fontFamily = FontFamily.Monospace,
          fontSize = 9.sp,
          modifier = Modifier.weight(1f)
        )

        Box(
          modifier = Modifier
            .background(TerminalSurface)
            .border(1.dp, PhosphorGreenDim)
            .clickable(onClick = onTrade)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = "[ TRD ]",
            color = PhosphorGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
