package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Commodity
import com.example.model.OrderBook
import com.example.model.OrderBookEntry
import com.example.model.OrderKind
import com.example.model.OrderType
import com.example.model.UserPortfolio
import com.example.ui.components.TerminalBox
import com.example.ui.components.TerminalButton
import com.example.ui.components.TerminalHeaderBar
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberTerminal
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.PhosphorGreenBright
import com.example.ui.theme.PhosphorGreenDark
import com.example.ui.theme.PhosphorGreenDim
import com.example.ui.theme.RedDim
import com.example.ui.theme.RedTerminal
import com.example.ui.theme.TerminalBlack
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalDarkBg
import com.example.ui.theme.TerminalSurface
import java.util.Locale
import kotlin.math.max

@Composable
fun OrderBookScreen(
  commodities: List<Commodity>,
  selectedCommodity: Commodity?,
  orderBooks: Map<String, OrderBook>,
  portfolio: UserPortfolio,
  tradeMessage: String?,
  onCommoditySelect: (Commodity) -> Unit,
  onExecuteTrade: (symbol: String, type: OrderType, kind: OrderKind, amount: Double, customPrice: Double?) -> Unit,
  onClearTradeMessage: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentCommodity = selectedCommodity ?: commodities.firstOrNull()
  val symbol = currentCommodity?.symbol ?: "GMA.GUA"
  val orderBook = orderBooks[symbol]

  var orderType by remember { mutableStateOf(OrderType.BUY) }
  var orderKind by remember { mutableStateOf(OrderKind.MARKET) }
  var amountInput by remember { mutableStateOf("1.0") }
  var limitPriceInput by remember(currentCommodity) {
    mutableStateOf(String.format(Locale.US, "%.2f", currentCommodity?.currentPrice ?: 1000.0))
  }

  val amountVal = amountInput.toDoubleOrNull() ?: 0.0
  val currentPrice = currentCommodity?.currentPrice ?: 1000.0
  val execPrice = if (orderKind == OrderKind.MARKET) currentPrice else (limitPriceInput.toDoubleOrNull() ?: currentPrice)
  val subtotal = amountVal * execPrice
  val existentialTax = subtotal * 0.005
  val grandTotal = subtotal + (if (orderType == OrderType.BUY) existentialTax else -existentialTax)

  val unitsOwned = portfolio.holdings[symbol]?.units ?: 0.0

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TerminalBlack)
      .verticalScroll(rememberScrollState())
  ) {
    TerminalHeaderBar(
      title = "BUKU PESANAN (ORDER BOOK)",
      subtitle = "TERMINAL LIKUIDITAS DAN KEDALAMAN PASAR",
      statusText = "MATCHING: ON"
    )

    // Commodity horizontal selector
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .background(TerminalDarkBg)
        .border(1.dp, TerminalBorder)
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      commodities.forEach { item ->
        val isSelected = item.symbol == symbol
        Box(
          modifier = Modifier
            .background(if (isSelected) PhosphorGreenDim.copy(alpha = 0.4f) else TerminalSurface)
            .border(1.dp, if (isSelected) PhosphorGreen else TerminalBorder)
            .clickable {
              onCommoditySelect(item)
              limitPriceInput = String.format(Locale.US, "%.2f", item.currentPrice)
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("select_orderbook_${item.symbol}")
        ) {
          Text(
            text = item.symbol,
            color = if (isSelected) PhosphorGreenBright else PhosphorGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
    ) {
      // Commodity current quotation bar
      if (currentCommodity != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .background(TerminalDarkBg)
            .border(1.dp, TerminalBorder)
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Column {
            Text(
              text = "${currentCommodity.symbol} // ${currentCommodity.name}",
              color = PhosphorGreen,
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "SATUAN: ${currentCommodity.unit}",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp
            )
          }
          Spacer(modifier = Modifier.weight(1f))
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = String.format(Locale.US, "%,.2f ABS", currentCommodity.currentPrice),
              color = PhosphorGreenBright,
              fontFamily = FontFamily.Monospace,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "DIMILIKI: ${String.format(Locale.US, "%.2f", unitsOwned)}",
              color = AmberTerminal,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // ORDER BOOK TABLE
      TerminalBox(
        title = "KEDALAMAN PASAR // $symbol",
        borderColor = TerminalBorder
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Table header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(TerminalSurface)
              .padding(horizontal = 4.dp, vertical = 3.dp)
          ) {
            Text(
              text = "HARGA (ABS)",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1.2f)
            )
            Text(
              text = "JUMLAH",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f)
            )
            Text(
              text = "TOTAL (ABS)",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1.2f)
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          // ASKS (Jual - Merah/Amber, reversed so lowest ask is near spread)
          val asks = orderBook?.asks?.take(5)?.reversed() ?: emptyList()
          asks.forEach { ask ->
            OrderBookRow(entry = ask, isAsk = true)
          }

          // SPREAD BAR
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .background(TerminalSurface)
              .border(1.dp, TerminalBorder)
              .padding(horizontal = 6.dp, vertical = 4.dp)
          ) {
            Text(
              text = "--- SPREAD PASAR ---",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
              text = String.format(Locale.US, "%.2f ABS (%.2f%%)", orderBook?.spread ?: 0.0, orderBook?.spreadPercent ?: 0.0),
              color = AmberTerminal,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // BIDS (Beli - Hijau)
          val bids = orderBook?.bids?.take(5) ?: emptyList()
          bids.forEach { bid ->
            OrderBookRow(entry = bid, isAsk = false)
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // TRADE EXECUTION CONSOLE
      TerminalBox(
        title = "KONSOL EKSEKUSI PESANAN",
        borderColor = if (orderType == OrderType.BUY) PhosphorGreen else AmberTerminal
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // BUY / SELL switch
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .background(if (orderType == OrderType.BUY) PhosphorGreenDim else TerminalSurface)
                .border(1.dp, PhosphorGreen)
                .clickable { orderType = OrderType.BUY }
                .padding(vertical = 8.dp)
                .testTag("tab_buy"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "[ + BELI (BUY) ]",
                color = if (orderType == OrderType.BUY) PhosphorGreenBright else PhosphorGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .background(if (orderType == OrderType.SELL) AmberDim else TerminalSurface)
                .border(1.dp, AmberTerminal)
                .clickable { orderType = OrderType.SELL }
                .padding(vertical = 8.dp)
                .testTag("tab_sell"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "[ - JUAL (SELL) ]",
                color = if (orderType == OrderType.SELL) AmberTerminal else AmberDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // MARKET vs LIMIT
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .background(if (orderKind == OrderKind.MARKET) TerminalBorder else TerminalSurface)
                .border(1.dp, TerminalBorder)
                .clickable { orderKind = OrderKind.MARKET }
                .padding(vertical = 5.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "HARGA PASAR (MARKET)",
                color = if (orderKind == OrderKind.MARKET) PhosphorGreenBright else PhosphorGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .background(if (orderKind == OrderKind.LIMIT) TerminalBorder else TerminalSurface)
                .border(1.dp, TerminalBorder)
                .clickable { orderKind = OrderKind.LIMIT }
                .padding(vertical = 5.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "PESANAN LIMIT",
                color = if (orderKind == OrderKind.LIMIT) PhosphorGreenBright else PhosphorGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Limit price input (if limit selected)
          if (orderKind == OrderKind.LIMIT) {
            Text(
              text = "HARGA LIMIT (ABS):",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            )
            OutlinedTextField(
              value = limitPriceInput,
              onValueChange = { limitPriceInput = it },
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("limit_price_input"),
              textStyle = TextStyle(
                color = PhosphorGreenBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
              ),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PhosphorGreen,
                unfocusedBorderColor = TerminalBorder,
                focusedContainerColor = TerminalSurface,
                unfocusedContainerColor = TerminalSurface
              )
            )
          }

          // Amount input + stepper
          Text(
            text = "JUMLAH UNIT (${currentCommodity?.unit ?: "UNIT"}):",
            color = PhosphorGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            OutlinedTextField(
              value = amountInput,
              onValueChange = { amountInput = it },
              modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
                .testTag("amount_input"),
              textStyle = TextStyle(
                color = PhosphorGreenBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
              ),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PhosphorGreen,
                unfocusedBorderColor = TerminalBorder,
                focusedContainerColor = TerminalSurface,
                unfocusedContainerColor = TerminalSurface
              )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
              modifier = Modifier
                .border(1.dp, TerminalBorder)
                .clickable {
                  val cur = amountInput.toDoubleOrNull() ?: 1.0
                  amountInput = max(0.5, cur - 1.0).toString()
                }
                .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
              Text(text = "-1", color = PhosphorGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
              modifier = Modifier
                .border(1.dp, TerminalBorder)
                .clickable {
                  val cur = amountInput.toDoubleOrNull() ?: 0.0
                  amountInput = (cur + 1.0).toString()
                }
                .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
              Text(text = "+1", color = PhosphorGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
              modifier = Modifier
                .border(1.dp, AmberTerminal)
                .clickable {
                  if (orderType == OrderType.BUY) {
                    val maxBuy = if (execPrice > 0) (portfolio.cashBalance / execPrice).toInt() else 0
                    amountInput = max(1, maxBuy).toString()
                  } else {
                    amountInput = unitsOwned.toString()
                  }
                }
                .padding(horizontal = 6.dp, vertical = 12.dp)
            ) {
              Text(text = "MAX", color = AmberTerminal, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Financial summary
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(TerminalSurface)
              .border(1.dp, TerminalBorder)
              .padding(8.dp)
          ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "SALDO TUNAI ABS:", color = PhosphorGreenDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
              Text(text = String.format(Locale.US, "%,.2f ABS", portfolio.cashBalance), color = PhosphorGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "PAJAK EKSISTENSIAL (0.5%):", color = PhosphorGreenDim, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
              Text(text = String.format(Locale.US, "%,.2f ABS", existentialTax), color = AmberTerminal, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "ESTIMASI TOTAL TRANSAKSI:", color = PhosphorGreenBright, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text(text = String.format(Locale.US, "%,.2f ABS", grandTotal), color = PhosphorGreenBright, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // EXECUTE BUTTON
          val actionColor = if (orderType == OrderType.BUY) PhosphorGreenBright else AmberTerminal
          TerminalButton(
            text = if (orderType == OrderType.BUY) "EKSEKUSI PEMBELIAN [$symbol]" else "EKSEKUSI PENJUALAN [$symbol]",
            onClick = {
              if (amountVal > 0) {
                onExecuteTrade(
                  symbol,
                  orderType,
                  orderKind,
                  amountVal,
                  if (orderKind == OrderKind.LIMIT) limitPriceInput.toDoubleOrNull() else null
                )
              }
            },
            color = actionColor,
            borderColor = actionColor,
            modifier = Modifier.fillMaxWidth(),
            testTag = "execute_trade_button"
          )

          // Result notification message
          if (tradeMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(TerminalDarkBg)
                .border(1.dp, if (tradeMessage.startsWith("SUKSES")) PhosphorGreen else RedTerminal)
                .clickable { onClearTradeMessage() }
                .padding(8.dp)
            ) {
              Text(
                text = tradeMessage,
                color = if (tradeMessage.startsWith("SUKSES")) PhosphorGreenBright else RedTerminal,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(80.dp))
    }
  }
}

@Composable
private fun OrderBookRow(
  entry: OrderBookEntry,
  isAsk: Boolean
) {
  val textColor = if (isAsk) RedTerminal else PhosphorGreen
  val depthBarColor = if (isAsk) RedDim.copy(alpha = 0.25f) else PhosphorGreenDark.copy(alpha = 0.35f)

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(20.dp)
      .padding(vertical = 1.dp)
  ) {
    // Relative depth bar
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(fraction = entry.depthPercent)
        .background(depthBarColor)
        .align(if (isAsk) Alignment.CenterEnd else Alignment.CenterStart)
    )

    // Data text
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 4.dp)
    ) {
      Text(
        text = String.format(Locale.US, "%,.2f", entry.price),
        color = textColor,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1.2f)
      )
      Text(
        text = String.format(Locale.US, "%.2f", entry.amount),
        color = PhosphorGreenDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = String.format(Locale.US, "%,.1f", entry.total),
        color = PhosphorGreen,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        modifier = Modifier.weight(1.2f)
      )
    }
  }
}
