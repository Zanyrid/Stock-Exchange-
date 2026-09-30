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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.components.RetroDetailedChart
import com.example.ui.components.TerminalBadge
import com.example.ui.components.TerminalBox
import com.example.ui.components.TerminalButton
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
import java.util.Locale

@Composable
fun CommodityDetailScreen(
  commodity: Commodity,
  onBack: () -> Unit,
  onOpenOrderBook: (Commodity) -> Unit,
  onSetAlert: (Commodity) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTimeframe by remember { mutableStateOf("5M") }
  val timeframes = remember { listOf("1M", "5M", "15M", "1H", "24H") }

  val isPositive = commodity.isPositive
  val changeColor = if (isPositive) PhosphorGreenBright else RedTerminal
  val changeSign = if (isPositive) "+" else ""

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TerminalBlack)
      .verticalScroll(rememberScrollState())
  ) {
    // Navigation & header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .background(TerminalSurface)
        .border(1.dp, TerminalBorder)
        .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
      Box(
        modifier = Modifier
          .clickable(onClick = onBack)
          .border(1.dp, PhosphorGreenDim)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "< [KEMBALI]",
          color = PhosphorGreen,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Text(
        text = "TERMINAL DETIL // ${commodity.symbol}",
        color = PhosphorGreenBright,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // Main commodity header
      TerminalBox(
        title = "IDENTIFIKASI KOMODITAS",
        borderColor = TerminalBorderGlow
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = commodity.symbol,
                color = PhosphorGreenBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = commodity.name,
                color = PhosphorGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = String.format(Locale.US, "%,.2f ABS", commodity.currentPrice),
                color = PhosphorGreenBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${commodity.unit} | %s%.2f%%".format(changeSign, commodity.change24hPercent),
                color = changeColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Timeframe buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        timeframes.forEach { tf ->
          val active = selectedTimeframe == tf
          Box(
            modifier = Modifier
              .weight(1f)
              .background(if (active) PhosphorGreenDim.copy(alpha = 0.4f) else TerminalSurface)
              .border(1.dp, if (active) PhosphorGreen else TerminalBorder)
              .clickable { selectedTimeframe = tf }
              .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = tf,
              color = if (active) PhosphorGreenBright else PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Detailed Interactive Line Chart
      RetroDetailedChart(
        data = commodity.priceHistory,
        unit = "ABS",
        height = 190.dp
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Lore & Absurd Description
      TerminalBox(
        title = "CATATAN HISTORIS & LORE",
        borderColor = TerminalBorder
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "\"${commodity.description}\"",
            color = PhosphorGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row {
            TerminalBadge(text = "KATEGORI: ${commodity.category}", color = CyanTerminal)
            Spacer(modifier = Modifier.width(8.dp))
            TerminalBadge(
              text = "VOLATILITAS: ${commodity.volatility.name}",
              color = if (commodity.volatility.name == "HIGH" || commodity.volatility.name == "CHAOTIC") AmberTerminal else PhosphorGreen
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Market Data Grid
      TerminalBox(
        title = "STATISTIK PASAR 24-JAM",
        borderColor = TerminalBorder
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          StatRow("HARGA PEMBUKAAN", String.format(Locale.US, "%,.2f ABS", commodity.openingPrice))
          StatRow("TERTINGGI (HIGH 24H)", String.format(Locale.US, "%,.2f ABS", commodity.high24h))
          StatRow("TERENDAH (LOW 24H)", String.format(Locale.US, "%,.2f ABS", commodity.low24h))
          StatRow("TOTAL VOLUME", String.format(Locale.US, "%,d UNIT", commodity.volume24h))
          StatRow("SATUAN TRANSAKSI", commodity.unit)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TerminalButton(
          text = "+ TARGET HARGA",
          onClick = { onSetAlert(commodity) },
          color = AmberTerminal,
          borderColor = AmberTerminal,
          modifier = Modifier.weight(1f),
          testTag = "set_alert_button"
        )

        TerminalButton(
          text = "ORDER BOOK >",
          onClick = { onOpenOrderBook(commodity) },
          color = PhosphorGreenBright,
          borderColor = PhosphorGreen,
          modifier = Modifier.weight(1f),
          testTag = "open_orderbook_button"
        )
      }

      Spacer(modifier = Modifier.height(80.dp))
    }
  }
}

@Composable
private fun StatRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      color = PhosphorGreenDim,
      fontFamily = FontFamily.Monospace,
      fontSize = 11.sp
    )
    Text(
      text = value,
      color = PhosphorGreen,
      fontFamily = FontFamily.Monospace,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
