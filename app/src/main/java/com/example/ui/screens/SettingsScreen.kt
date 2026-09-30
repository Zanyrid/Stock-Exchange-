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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExchangeSettings
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

@Composable
fun SettingsScreen(
  settings: ExchangeSettings,
  terminalLogs: List<String>,
  isTesting: Boolean,
  testResult: Pair<Boolean, String>?,
  onSaveSettings: (ExchangeSettings) -> Unit,
  onTestConnection: (String) -> Unit,
  onResetPortfolio: () -> Unit,
  onReplayBoot: () -> Unit,
  modifier: Modifier = Modifier
) {
  var restUrl by remember(settings) { mutableStateOf(settings.restApiUrl) }
  var wsUrl by remember(settings) { mutableStateOf(settings.webSocketUrl) }
  var useLocalSim by remember(settings) { mutableStateOf(settings.useLocalSimulation) }
  var scanlinesEnabled by remember(settings) { mutableStateOf(settings.enableScanlines) }
  var cursorEnabled by remember(settings) { mutableStateOf(settings.enableBlinkingCursor) }
  var hapticEnabled by remember(settings) { mutableStateOf(settings.enableHapticFeedback) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(TerminalBlack)
      .verticalScroll(rememberScrollState())
  ) {
    TerminalHeaderBar(
      title = "KONFIGURASI SISTEM & JARINGAN",
      subtitle = "PENGATURAN PROTOKOL BURSA & TAMPILAN",
      statusText = "SYS: CONFIG"
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // API & WEBSOCKET CONFIGURATION
      TerminalBox(
        title = "PROTOKOL ENDPOINT DATA",
        borderColor = PhosphorGreen
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "URL REST API KOMODITAS:",
            color = PhosphorGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
          )
          OutlinedTextField(
            value = restUrl,
            onValueChange = {
              restUrl = it
              onSaveSettings(settings.copy(restApiUrl = it))
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .testTag("rest_api_url_input"),
            textStyle = TextStyle(
              color = PhosphorGreenBright,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.5.sp
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = PhosphorGreen,
              unfocusedBorderColor = TerminalBorder,
              focusedContainerColor = TerminalSurface,
              unfocusedContainerColor = TerminalSurface
            )
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "URL WEBSOCKET REAL-TIME FEED:",
            color = PhosphorGreenDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
          )
          OutlinedTextField(
            value = wsUrl,
            onValueChange = {
              wsUrl = it
              onSaveSettings(settings.copy(webSocketUrl = it))
            },
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .testTag("websocket_url_input"),
            textStyle = TextStyle(
              color = PhosphorGreenBright,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.5.sp
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = PhosphorGreen,
              unfocusedBorderColor = TerminalBorder,
              focusedContainerColor = TerminalSurface,
              unfocusedContainerColor = TerminalSurface
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Ping Test Button
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            TerminalButton(
              text = if (isTesting) "SEDANG PING HOST..." else "UJI KONEKSI (PING URL)",
              onClick = { onTestConnection(restUrl) },
              enabled = !isTesting && restUrl.isNotBlank(),
              color = AmberTerminal,
              borderColor = AmberTerminal,
              modifier = Modifier.weight(1f),
              testTag = "ping_test_button"
            )
          }

          if (testResult != null) {
            Spacer(modifier = Modifier.height(6.dp))
            val isSuccess = testResult.first
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(TerminalSurface)
                .border(1.dp, if (isSuccess) PhosphorGreen else AmberTerminal)
                .padding(8.dp)
            ) {
              Text(
                text = if (isSuccess) "HASIL PING: ${testResult.second}" else "PING GAGAL: ${testResult.second}\n(OTOMATIS BEROPERASI MENGGUNAKAN SIMULASI LOKAL RETRO)",
                color = if (isSuccess) PhosphorGreenBright else AmberTerminal,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Data Mode Toggle
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .background(TerminalSurface)
              .border(1.dp, TerminalBorder)
              .padding(8.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "SUMBER DATA: SIMULASI LOKAL",
                color = PhosphorGreenBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (useLocalSim) "Generator kurs fiktif internal aktif (1984 engine)" else "Mendengarkan REST/WebSocket eksternal",
                color = PhosphorGreenDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp
              )
            }

            Switch(
              checked = useLocalSim,
              onCheckedChange = {
                useLocalSim = it
                onSaveSettings(settings.copy(useLocalSimulation = it))
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = PhosphorGreenBright,
                checkedTrackColor = TerminalBorderGlow,
                uncheckedThumbColor = PhosphorGreenDim,
                uncheckedTrackColor = TerminalDarkBg
              )
            )
          }
        }
      }

      // CRT & VINTAGE VISUALS
      TerminalBox(
        title = "EFEK VISUAL TERMINAL CRT",
        borderColor = TerminalBorder
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // Scanlines toggle
          SettingToggleRow(
            title = "GARIS SCANLINE CRT",
            subtitle = "Efek garis tabung sinar katoda (Cathode Ray Tube)",
            checked = scanlinesEnabled,
            onCheckedChange = {
              scanlinesEnabled = it
              onSaveSettings(settings.copy(enableScanlines = it))
            }
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Blinking cursor toggle
          SettingToggleRow(
            title = "KURSOR BERKEDIP (BLINKING CURSOR)",
            subtitle = "Animasi prompt retro konsol 1980-an",
            checked = cursorEnabled,
            onCheckedChange = {
              cursorEnabled = it
              onSaveSettings(settings.copy(enableBlinkingCursor = it))
            }
          )

          Spacer(modifier = Modifier.height(6.dp))

          // Haptic Feedback
          SettingToggleRow(
            title = "GETARAN HAPTIK KEYBOARD",
            subtitle = "Umpan balik klik tactile tombol terminal",
            checked = hapticEnabled,
            onCheckedChange = {
              hapticEnabled = it
              onSaveSettings(settings.copy(enableHapticFeedback = it))
            }
          )
        }
      }

      // SYSTEM ACTIONS
      TerminalBox(
        title = "AKSI SISTEM & PEMELIHARAAN",
        borderColor = TerminalBorder
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TerminalButton(
            text = "PUTAR ULANG BOOT SEQUENCE RETRO",
            onClick = onReplayBoot,
            color = PhosphorGreen,
            borderColor = TerminalBorder,
            modifier = Modifier.fillMaxWidth(),
            testTag = "replay_boot_button"
          )

          TerminalButton(
            text = "RESET SALDO & PORTOFOLIO KE 15,000 ABS",
            onClick = onResetPortfolio,
            color = RedTerminal,
            borderColor = RedTerminal.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            testTag = "reset_portfolio_button"
          )
        }
      }

      // KERNEL LOGS VIEWER
      TerminalBox(
        title = "CATATAN KERNEL (SYSTEM LOGS)",
        borderColor = TerminalBorder
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .verticalScroll(rememberScrollState())
        ) {
          terminalLogs.forEach { log ->
            Text(
              text = log,
              color = if (log.contains("ALERT")) AmberTerminal else if (log.contains("TRADE")) PhosphorGreenBright else PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.5.sp,
              lineHeight = 13.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(80.dp))
    }
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .background(TerminalDarkBg)
      .border(1.dp, TerminalBorder)
      .padding(horizontal = 8.dp, vertical = 6.dp)
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        color = PhosphorGreenBright,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = subtitle,
        color = PhosphorGreenDim,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.5.sp
      )
    }

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = PhosphorGreenBright,
        checkedTrackColor = TerminalBorderGlow,
        uncheckedThumbColor = PhosphorGreenDim,
        uncheckedTrackColor = TerminalDarkBg
      )
    )
  }
}
