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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Commodity
import com.example.model.OrderType
import com.example.model.PortfolioHolding
import com.example.model.TradeOrder
import com.example.model.UserPortfolio
import com.example.ui.components.TerminalBadge
import com.example.ui.components.TerminalBox
import com.example.ui.components.TerminalButton
import com.example.ui.components.TerminalHeaderBar
import com.example.ui.theme.AmberTerminal
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PortfolioScreen(
  portfolio: UserPortfolio,
  commodities: List<Commodity>,
  onTradeSymbol: (String) -> Unit,
  onResetPortfolio: () -> Unit,
  modifier: Modifier = Modifier
) {
  val priceMap = remember(commodities) { commodities.associate { it.symbol to it.currentPrice } }
  val totalNetWorth = portfolio.totalNetWorth(priceMap)
  val totalInvested = portfolio.holdings.values.sumOf { it.units * it.avgBuyPrice }
  val totalCurrentHoldingsValue = portfolio.holdings.values.sumOf {
    val cur = priceMap[it.symbol] ?: it.avgBuyPrice
    it.currentValue(cur)
  }
  val totalPnl = totalCurrentHoldingsValue - totalInvested
  val totalPnlPercent = if (totalInvested > 0) (totalPnl / totalInvested) * 100.0 else 0.0

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TerminalBlack)
  ) {
    TerminalHeaderBar(
      title = "DOMPET & PORTOFOLIO PIALANG",
      subtitle = "PENCATATAN KEKAYAAN TAK BERWUJUD",
      statusText = "AUDIT: OK"
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Net Worth Card
      item {
        TerminalBox(
          title = "NERACA KEKAYAAN BERSIH",
          borderColor = TerminalBorderGlow
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "TOTAL NET WORTH:",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            )
            Text(
              text = String.format(Locale.US, "%,.2f ABS", totalNetWorth),
              color = PhosphorGreenBright,
              fontFamily = FontFamily.Monospace,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "SALDO TUNAI (CASH):", color = PhosphorGreenDim, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
              Text(text = String.format(Locale.US, "%,.2f ABS", portfolio.cashBalance), color = PhosphorGreen, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "NILAI ASET TAK BERWUJUD:", color = PhosphorGreenDim, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
              Text(text = String.format(Locale.US, "%,.2f ABS", totalCurrentHoldingsValue), color = PhosphorGreen, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "UNREALIZED P&L TOTAL:", color = PhosphorGreenDim, fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
              val pnlColor = if (totalPnl >= 0) PhosphorGreenBright else RedTerminal
              val pnlSign = if (totalPnl >= 0) "+" else ""
              Text(
                text = String.format(Locale.US, "%s%,.2f ABS (%s%.2f%%)", pnlSign, totalPnl, pnlSign, totalPnlPercent),
                color = pnlColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Holdings Section
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "DAFTAR KEPEMILIKAN ASET (${portfolio.holdings.size}):",
            color = PhosphorGreenBright,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      if (portfolio.holdings.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(TerminalDarkBg)
              .border(1.dp, TerminalBorder)
              .padding(14.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "[BELUM MEMILIKI KOMODITAS TAK BERWUJUD]",
              color = PhosphorGreenDark,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            )
          }
        }
      } else {
        items(portfolio.holdings.values.toList()) { holding ->
          val com = commodities.find { it.symbol == holding.symbol }
          val curPrice = com?.currentPrice ?: holding.avgBuyPrice
          HoldingItemCard(
            holding = holding,
            currentPrice = curPrice,
            commodityName = com?.name ?: holding.symbol,
            unit = com?.unit ?: "UNIT",
            onTrade = { onTradeSymbol(holding.symbol) }
          )
        }
      }

      // Trade History Section
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "LOG TRANSAKSI TERAKHIR (${portfolio.tradeHistory.size}):",
            color = PhosphorGreenBright,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.weight(1f))
          Box(
            modifier = Modifier
              .border(1.dp, TerminalBorder)
              .clickable(onClick = onResetPortfolio)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "[ RESET DATA ]",
              color = AmberTerminal,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp
            )
          }
        }
      }

      if (portfolio.tradeHistory.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(TerminalDarkBg)
              .border(1.dp, TerminalBorder)
              .padding(12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "[BELUM ADA TRANSAKSI DIEKSEKUSI]",
              color = PhosphorGreenDark,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            )
          }
        }
      } else {
        items(portfolio.tradeHistory) { trade ->
          TradeHistoryRow(trade = trade)
        }
      }

      item {
        Spacer(modifier = Modifier.height(80.dp))
      }
    }
  }
}

@Composable
private fun HoldingItemCard(
  holding: PortfolioHolding,
  currentPrice: Double,
  commodityName: String,
  unit: String,
  onTrade: () -> Unit
) {
  val curVal = holding.currentValue(currentPrice)
  val pnl = holding.profitLoss(currentPrice)
  val pnlPercent = holding.profitLossPercent(currentPrice)
  val pnlColor = if (pnl >= 0) PhosphorGreenBright else RedTerminal
  val pnlSign = if (pnl >= 0) "+" else ""

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(TerminalDarkBg)
      .border(1.dp, TerminalBorder)
      .padding(10.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
          text = holding.symbol,
          color = PhosphorGreenBright,
          fontFamily = FontFamily.Monospace,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = commodityName,
          color = PhosphorGreen,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          modifier = Modifier.weight(1f)
        )
        Box(
          modifier = Modifier
            .border(1.dp, PhosphorGreenDim)
            .clickable(onClick = onTrade)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(text = "[ JUAL/BELI ]", color = PhosphorGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
          Text(
            text = "JUMLAH: ${String.format(Locale.US, "%.2f", holding.units)} $unit",
            color = PhosphorGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
          )
          Text(
            text = "AVG BUY: ${String.format(Locale.US, "%,.2f ABS", holding.avgBuyPrice)}",
            color = PhosphorGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = String.format(Locale.US, "%,.2f ABS", curVal),
            color = PhosphorGreenBright,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = String.format(Locale.US, "P&L: %s%,.1f (%s%.1f%%)", pnlSign, pnl, pnlSign, pnlPercent),
            color = pnlColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.5.sp
          )
        }
      }
    }
  }
}

@Composable
private fun TradeHistoryRow(trade: TradeOrder) {
  val isBuy = trade.type == OrderType.BUY
  val tagColor = if (isBuy) PhosphorGreen else AmberTerminal
  val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(trade.timestamp))

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(TerminalDarkBg)
      .border(1.dp, TerminalBorder)
      .padding(horizontal = 8.dp, vertical = 6.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "[$timeStr]",
        color = PhosphorGreenDark,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp
      )
      Spacer(modifier = Modifier.width(6.dp))
      TerminalBadge(text = if (isBuy) "BUY" else "SELL", color = tagColor)
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "${trade.amount} ${trade.symbol}",
        color = PhosphorGreenBright,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = String.format(Locale.US, "%,.2f ABS", trade.totalCost),
        color = tagColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
