package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Commodity
import com.example.ui.components.CrtScanlineOverlay
import com.example.ui.screens.CommodityDetailScreen
import com.example.ui.screens.CommodityListScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.OrderBookScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.PriceAlertsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.BursaAbsurdTheme
import com.example.ui.theme.PhosphorGreen
import com.example.ui.theme.PhosphorGreenBright
import com.example.ui.theme.PhosphorGreenDim
import com.example.ui.theme.TerminalBlack
import com.example.ui.theme.TerminalBorder
import com.example.ui.theme.TerminalDarkBg
import com.example.ui.theme.TerminalSurface
import com.example.ui.viewmodel.ExchangeViewModel

enum class MainTab(val label: String, val tag: String) {
  MARKET("PASAR", "tab_nav_market"),
  ORDER_BOOK("BUKU", "tab_nav_orderbook"),
  LEADERBOARD("RANK", "tab_nav_leaderboard"),
  ALERTS("NOTIF", "tab_nav_alerts"),
  PORTFOLIO("PORT", "tab_nav_portfolio"),
  SETTINGS("CFG", "tab_nav_settings")
}

class MainActivity : ComponentActivity() {

  private val viewModel: ExchangeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val targetSymbolFromIntent = intent?.getStringExtra("ALERT_TRIGGERED_SYMBOL")

    setContent {
      BursaAbsurdTheme {
        MainApp(
          viewModel = viewModel,
          initialAlertSymbol = targetSymbolFromIntent
        )
      }
    }
  }
}

@Composable
fun MainApp(
  viewModel: ExchangeViewModel,
  initialAlertSymbol: String? = null
) {
  var showSplash by rememberSaveable { mutableStateOf(true) }
  var currentTab by rememberSaveable { mutableStateOf(MainTab.MARKET) }
  var detailCommodity by remember { mutableStateOf<Commodity?>(null) }

  val commodities by viewModel.commodities.collectAsStateWithLifecycle()
  val selectedCommodity by viewModel.selectedCommodity.collectAsStateWithLifecycle()
  val orderBooks by viewModel.orderBooks.collectAsStateWithLifecycle()
  val portfolio by viewModel.portfolio.collectAsStateWithLifecycle()
  val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
  val alerts by viewModel.alerts.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
  val tradeMessage by viewModel.tradeMessage.collectAsStateWithLifecycle()
  val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
  val testResult by viewModel.testConnectionResult.collectAsStateWithLifecycle()

  LaunchedEffect(initialAlertSymbol) {
    if (initialAlertSymbol != null) {
      val found = commodities.find { it.symbol == initialAlertSymbol }
      if (found != null) {
        viewModel.selectCommodity(found)
        currentTab = MainTab.ORDER_BOOK
      } else {
        currentTab = MainTab.ALERTS
      }
    }
  }

  // Handle system back button
  BackHandler(enabled = detailCommodity != null || currentTab != MainTab.MARKET) {
    if (detailCommodity != null) {
      detailCommodity = null
    } else {
      currentTab = MainTab.MARKET
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(TerminalBlack)
  ) {
    if (showSplash) {
      SplashScreen(
        onComplete = { showSplash = false }
      )
    } else {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TerminalBlack,
        bottomBar = {
          RetroTerminalBottomNav(
            currentTab = currentTab,
            onTabSelect = { tab ->
              detailCommodity = null
              currentTab = tab
            },
            alertsCount = alerts.count { !it.isTriggered }
          )
        }
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
          if (detailCommodity != null) {
            val freshCommodity = commodities.find { it.id == detailCommodity!!.id } ?: detailCommodity!!
            CommodityDetailScreen(
              commodity = freshCommodity,
              onBack = { detailCommodity = null },
              onOpenOrderBook = { com ->
                viewModel.selectCommodity(com)
                detailCommodity = null
                currentTab = MainTab.ORDER_BOOK
              },
              onSetAlert = { com ->
                detailCommodity = null
                currentTab = MainTab.ALERTS
              }
            )
          } else {
            when (currentTab) {
              MainTab.MARKET -> {
                CommodityListScreen(
                  commodities = commodities,
                  alertsCount = alerts.count { !it.isTriggered },
                  onCommodityClick = { com ->
                    detailCommodity = com
                  },
                  onQuickTradeClick = { com ->
                    viewModel.selectCommodity(com)
                    currentTab = MainTab.ORDER_BOOK
                  }
                )
              }

              MainTab.ORDER_BOOK -> {
                OrderBookScreen(
                  commodities = commodities,
                  selectedCommodity = selectedCommodity,
                  orderBooks = orderBooks,
                  portfolio = portfolio,
                  tradeMessage = tradeMessage,
                  onCommoditySelect = { com -> viewModel.selectCommodity(com) },
                  onExecuteTrade = { symbol, type, kind, amount, customPrice ->
                    viewModel.executeTrade(symbol, type, kind, amount, customPrice)
                  },
                  onClearTradeMessage = { viewModel.clearTradeMessage() }
                )
              }

              MainTab.LEADERBOARD -> {
                LeaderboardScreen(traders = leaderboard)
              }

              MainTab.ALERTS -> {
                PriceAlertsScreen(
                  commodities = commodities,
                  alerts = alerts,
                  onAddAlert = { sym, price, cond ->
                    viewModel.addPriceAlert(sym, price, cond)
                  },
                  onRemoveAlert = { id -> viewModel.removePriceAlert(id) }
                )
              }

              MainTab.PORTFOLIO -> {
                PortfolioScreen(
                  portfolio = portfolio,
                  commodities = commodities,
                  onTradeSymbol = { sym ->
                    commodities.find { it.symbol == sym }?.let { viewModel.selectCommodity(it) }
                    currentTab = MainTab.ORDER_BOOK
                  },
                  onResetPortfolio = { viewModel.resetPortfolio() }
                )
              }

              MainTab.SETTINGS -> {
                SettingsScreen(
                  settings = settings,
                  terminalLogs = terminalLogs,
                  isTesting = isTesting,
                  testResult = testResult,
                  onSaveSettings = { viewModel.updateSettings(it) },
                  onTestConnection = { viewModel.testConnection(it) },
                  onResetPortfolio = { viewModel.resetPortfolio() },
                  onReplayBoot = { showSplash = true }
                )
              }
            }
          }

          // Optional CRT Scanlines Layer based on settings
          if (settings.enableScanlines) {
            CrtScanlineOverlay()
          }
        }
      }
    }
  }
}

@Composable
fun RetroTerminalBottomNav(
  currentTab: MainTab,
  onTabSelect: (MainTab) -> Unit,
  alertsCount: Int,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(TerminalDarkBg)
      .border(1.dp, TerminalBorder)
      .navigationBarsPadding()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      MainTab.values().forEach { tab ->
        val isSelected = currentTab == tab
        Box(
          modifier = Modifier
            .background(if (isSelected) PhosphorGreenDim.copy(alpha = 0.35f) else TerminalSurface)
            .border(1.dp, if (isSelected) PhosphorGreen else TerminalBorder)
            .clickable { onTabSelect(tab) }
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag(tab.tag),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = if (isSelected) ">${tab.label}<" else tab.label,
              color = if (isSelected) PhosphorGreenBright else PhosphorGreenDim,
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (tab == MainTab.ALERTS && alertsCount > 0) {
              Text(
                text = "($alertsCount)",
                color = PhosphorGreenBright,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp)
              )
            }
          }
        }
      }
    }
  }
}
