package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NotificationHelper
import com.example.model.AlertCondition
import com.example.model.Commodity
import com.example.model.PriceAlert
import com.example.ui.components.TerminalBadge
import com.example.ui.components.TerminalBox
import com.example.ui.components.TerminalButton
import com.example.ui.components.TerminalHeaderBar
import com.example.ui.theme.AmberDim
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
fun PriceAlertsScreen(
  commodities: List<Commodity>,
  alerts: List<PriceAlert>,
  onAddAlert: (symbol: String, targetPrice: Double, condition: AlertCondition) -> Unit,
  onRemoveAlert: (alertId: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val notificationHelper = remember { NotificationHelper(context) }
  var hasNotificationPermission by remember { mutableStateOf(notificationHelper.canPostNotification()) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasNotificationPermission = isGranted
  }

  var selectedSymbol by remember { mutableStateOf(commodities.firstOrNull()?.symbol ?: "GMA.GUA") }
  val currentCommodity = commodities.find { it.symbol == selectedSymbol }
  var targetPriceInput by remember(selectedSymbol) {
    mutableStateOf(String.format(Locale.US, "%.2f", (currentCommodity?.currentPrice ?: 1000.0) * 1.05))
  }
  var selectedCondition by remember { mutableStateOf(AlertCondition.ABOVE_OR_EQUAL) }

  val activeAlerts = alerts.filter { !it.isTriggered }
  val triggeredAlerts = alerts.filter { it.isTriggered }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TerminalBlack)
  ) {
    TerminalHeaderBar(
      title = "PENGAWAS TARGET HARGA (ALERTS)",
      subtitle = "NOTIFIKASI SISTEM REAL-TIME REALISASI KURS",
      statusText = if (hasNotificationPermission) "NOTIF: SIAP" else "NOTIF: DIBATASI",
      alertsCount = activeAlerts.size
    )

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Permission Banner if needed
      if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(TerminalSurface)
              .border(1.dp, AmberTerminal)
              .padding(10.dp)
          ) {
            Column {
              Text(
                text = ">>> IZIN NOTIFIKASI ANDROID DIPERLUKAN <<<",
                color = AmberTerminal,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Agar sistem dapat membunyikan peringatan saat harga menembus target, aktifkan izin notifikasi.",
                color = PhosphorGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
              )
              Spacer(modifier = Modifier.height(6.dp))
              TerminalButton(
                text = "BERIKAN IZIN NOTIFIKASI",
                onClick = {
                  permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                },
                color = AmberTerminal,
                borderColor = AmberTerminal,
                testTag = "grant_notification_permission_button"
              )
            }
          }
        }
      }

      // Add Alert Form
      item {
        TerminalBox(
          title = "PASANG TARGET BARU",
          borderColor = PhosphorGreen
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "PILIH KOMODITAS TAK BERWUJUD:",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            )

            // Commodity Selector
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 6.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              commodities.forEach { item ->
                val isSelected = item.symbol == selectedSymbol
                Box(
                  modifier = Modifier
                    .background(if (isSelected) PhosphorGreenDim.copy(alpha = 0.4f) else TerminalSurface)
                    .border(1.dp, if (isSelected) PhosphorGreen else TerminalBorder)
                    .clickable {
                      selectedSymbol = item.symbol
                      targetPriceInput = String.format(Locale.US, "%.2f", item.currentPrice * 1.05)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = item.symbol,
                    color = if (isSelected) PhosphorGreenBright else PhosphorGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                }
              }
            }

            if (currentCommodity != null) {
              Text(
                text = "KURS TERKINI: ${String.format(Locale.US, "%,.2f ABS", currentCommodity.currentPrice)} / ${currentCommodity.unit}",
                color = PhosphorGreenBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Condition Selector
            Text(
              text = "KONDISI PEMICU:",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(if (selectedCondition == AlertCondition.ABOVE_OR_EQUAL) PhosphorGreenDim.copy(alpha = 0.4f) else TerminalSurface)
                  .border(1.dp, if (selectedCondition == AlertCondition.ABOVE_OR_EQUAL) PhosphorGreen else TerminalBorder)
                  .clickable { selectedCondition = AlertCondition.ABOVE_OR_EQUAL }
                  .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = ">= NAIK MELAMPAUI",
                  color = if (selectedCondition == AlertCondition.ABOVE_OR_EQUAL) PhosphorGreenBright else PhosphorGreenDim,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .background(if (selectedCondition == AlertCondition.BELOW_OR_EQUAL) AmberDim.copy(alpha = 0.4f) else TerminalSurface)
                  .border(1.dp, if (selectedCondition == AlertCondition.BELOW_OR_EQUAL) AmberTerminal else TerminalBorder)
                  .clickable { selectedCondition = AlertCondition.BELOW_OR_EQUAL }
                  .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "<= TURUN DI BAWAH",
                  color = if (selectedCondition == AlertCondition.BELOW_OR_EQUAL) AmberTerminal else PhosphorGreenDim,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Target Price Input
            Text(
              text = "TARGET HARGA (ABS):",
              color = PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            )
            OutlinedTextField(
              value = targetPriceInput,
              onValueChange = { targetPriceInput = it },
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("target_price_input"),
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

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              TerminalButton(
                text = "SIMULASIKAN NOTIF TEST",
                onClick = {
                  notificationHelper.sendPriceAlertNotification(
                    alertId = "test_alert",
                    symbol = selectedSymbol,
                    name = currentCommodity?.name ?: selectedSymbol,
                    targetPrice = targetPriceInput.toDoubleOrNull() ?: 1500.0,
                    currentPrice = currentCommodity?.currentPrice ?: 1500.0,
                    isAbove = selectedCondition == AlertCondition.ABOVE_OR_EQUAL
                  )
                },
                color = AmberTerminal,
                borderColor = AmberTerminal,
                modifier = Modifier.weight(1f),
                testTag = "test_alert_notification_button"
              )

              TerminalButton(
                text = "+ AKTIFKAN PENGAWAS",
                onClick = {
                  val price = targetPriceInput.toDoubleOrNull()
                  if (price != null && price > 0) {
                    onAddAlert(selectedSymbol, price, selectedCondition)
                  }
                },
                color = PhosphorGreenBright,
                borderColor = PhosphorGreen,
                modifier = Modifier.weight(1.2f),
                testTag = "activate_alert_button"
              )
            }
          }
        }
      }

      // Active Alerts List
      item {
        Text(
          text = "PENGAWAS AKTIF (${activeAlerts.size}):",
          color = PhosphorGreenBright,
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }

      if (activeAlerts.isEmpty()) {
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
              text = "[TIDAK ADA TARGET HARGA AKTIF]",
              color = PhosphorGreenDark,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            )
          }
        }
      } else {
        items(activeAlerts) { alert ->
          val com = commodities.find { it.symbol == alert.symbol }
          AlertRow(
            alert = alert,
            currentPrice = com?.currentPrice ?: 0.0,
            onDelete = { onRemoveAlert(alert.id) }
          )
        }
      }

      // Triggered Alerts History
      if (triggeredAlerts.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "RIWAYAT TARGET TERCAPAI (${triggeredAlerts.size}):",
            color = AmberTerminal,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }

        items(triggeredAlerts) { alert ->
          TriggeredAlertRow(alert = alert, onDelete = { onRemoveAlert(alert.id) })
        }
      }

      item {
        Spacer(modifier = Modifier.height(80.dp))
      }
    }
  }
}

@Composable
private fun AlertRow(
  alert: PriceAlert,
  currentPrice: Double,
  onDelete: () -> Unit
) {
  val conditionSymbol = if (alert.condition == AlertCondition.ABOVE_OR_EQUAL) ">=" else "<="

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(TerminalDarkBg)
      .border(1.dp, TerminalBorder)
      .padding(10.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = alert.symbol,
            color = PhosphorGreenBright,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(6.dp))
          TerminalBadge(
            text = "$conditionSymbol ${String.format(Locale.US, "%,.2f ABS", alert.targetPrice)}",
            color = if (alert.condition == AlertCondition.ABOVE_OR_EQUAL) PhosphorGreen else AmberTerminal
          )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = "HARGA SEKARANG: ${String.format(Locale.US, "%,.2f ABS", currentPrice)}",
          color = PhosphorGreenDim,
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp
        )
      }

      Box(
        modifier = Modifier
          .border(1.dp, RedTerminal)
          .clickable(onClick = onDelete)
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "[ HAPUS ]",
          color = RedTerminal,
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp
        )
      }
    }
  }
}

@Composable
private fun TriggeredAlertRow(
  alert: PriceAlert,
  onDelete: () -> Unit
) {
  val timeStr = if (alert.triggeredAt != null) {
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(alert.triggeredAt))
  } else "--:--"

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(TerminalSurface)
      .border(1.dp, AmberTerminal.copy(alpha = 0.5f))
      .padding(8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "[TERPICU $timeStr] ${alert.symbol}",
          color = AmberTerminal,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Target: ${String.format(Locale.US, "%,.2f", alert.targetPrice)} | Terealisasi @ ${String.format(Locale.US, "%,.2f ABS", alert.triggeredPrice ?: alert.targetPrice)}",
          color = PhosphorGreenDim,
          fontFamily = FontFamily.Monospace,
          fontSize = 9.5.sp
        )
      }

      Box(
        modifier = Modifier
          .border(1.dp, TerminalBorder)
          .clickable(onClick = onDelete)
          .padding(horizontal = 6.dp, vertical = 3.dp)
      ) {
        Text(text = "[X]", color = PhosphorGreenDim, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
      }
    }
  }
}
