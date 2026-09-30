package com.example.data

import com.example.model.AlertCondition
import com.example.model.Commodity
import com.example.model.OrderBook
import com.example.model.OrderBookEntry
import com.example.model.OrderKind
import com.example.model.OrderType
import com.example.model.PortfolioHolding
import com.example.model.PriceAlert
import com.example.model.TradeOrder
import com.example.model.Trader
import com.example.model.UserPortfolio
import com.example.model.Volatility
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class ExchangeSettings(
  val restApiUrl: String = "https://api.bursa-absurd.net/v1/ticker",
  val webSocketUrl: String = "wss://ws.bursa-absurd.net/feed",
  val useLocalSimulation: Boolean = true,
  val enableScanlines: Boolean = true,
  val enableCrtGlow: Boolean = true,
  val enableBlinkingCursor: Boolean = true,
  val enableHapticFeedback: Boolean = true
)

class AbsurdExchangeRepository(
  private val notificationHelper: NotificationHelper,
  private val scope: CoroutineScope
) {

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(3, TimeUnit.SECONDS)
    .readTimeout(3, TimeUnit.SECONDS)
    .build()

  private val _commodities = MutableStateFlow<List<Commodity>>(initialCommodities())
  val commodities: StateFlow<List<Commodity>> = _commodities.asStateFlow()

  private val _selectedCommodity = MutableStateFlow<Commodity?>(initialCommodities().first())
  val selectedCommodity: StateFlow<Commodity?> = _selectedCommodity.asStateFlow()

  private val _orderBooks = MutableStateFlow<Map<String, OrderBook>>(emptyMap())
  val orderBooks: StateFlow<Map<String, OrderBook>> = _orderBooks.asStateFlow()

  private val _portfolio = MutableStateFlow(
    UserPortfolio(
      cashBalance = 12500.0,
      holdings = mapOf(
        "GMA.GUA" to PortfolioHolding("GMA.GUA", units = 2.5, avgBuyPrice = 1420.0),
        "AWN.KUM" to PortfolioHolding("AWN.KUM", units = 5.0, avgBuyPrice = 750.0)
      )
    )
  )
  val portfolio: StateFlow<UserPortfolio> = _portfolio.asStateFlow()

  private val _leaderboard = MutableStateFlow<List<Trader>>(emptyList())
  val leaderboard: StateFlow<List<Trader>> = _leaderboard.asStateFlow()

  private val _alerts = MutableStateFlow<List<PriceAlert>>(emptyList())
  val alerts: StateFlow<List<PriceAlert>> = _alerts.asStateFlow()

  private val _settings = MutableStateFlow(ExchangeSettings())
  val settings: StateFlow<ExchangeSettings> = _settings.asStateFlow()

  private val _terminalLogs = MutableStateFlow<List<String>>(
    listOf(
      "[03:40:12] COMMODEX-84 SYSTEM KERNEL INITIALIZED",
      "[03:40:15] FEED CONNECTED: SIMULASI BURSA ABSURD REAL-TIME",
      "[03:40:16] INTANGIBLE ASSETS REGISTRY VERIFIED: 8 TICKERS ACTIVE"
    )
  )
  val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

  private val _connectionStatus = MutableStateFlow("SIMULASI REAL-TIME AKTIF")
  val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

  init {
    updateLeaderboard()
    regenerateAllOrderBooks()
    startRealTimeTickLoop()
  }

  fun selectCommodity(commodity: Commodity) {
    _selectedCommodity.value = commodity
  }

  fun updateSettings(newSettings: ExchangeSettings) {
    _settings.value = newSettings
    appendLog("SYS-CFG: SETTINGS UPDATED [SIM=${newSettings.useLocalSimulation}]")
  }

  suspend fun testConnection(url: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    appendLog("PING: INITIATING HTTP PROBE TO $url ...")
    try {
      val request = Request.Builder().url(url).build()
      val response = httpClient.newCall(request).execute()
      val code = response.code
      response.close()
      val msg = "RESPONSE CODE $code FROM HOST"
      appendLog("PING SUCCESS: $msg")
      Pair(true, msg)
    } catch (e: Exception) {
      val errorMsg = e.message ?: "KONEKSI GAGAL ATAU HOST TIDAK TERJANGKAU"
      appendLog("PING ERROR: $errorMsg (FALLBACK KE SIMULATOR LOKAL)")
      Pair(false, errorMsg)
    }
  }

  private fun startRealTimeTickLoop() {
    scope.launch(Dispatchers.Default) {
      while (isActive) {
        delay(2500L) // Real-time tick interval
        tickPrices()
      }
    }
  }

  private fun tickPrices() {
    val updated = _commodities.value.map { commodity ->
      val deltaPercent = when (commodity.volatility) {
        Volatility.LOW -> (Random.nextDouble(-0.8, 0.9))
        Volatility.MEDIUM -> (Random.nextDouble(-2.0, 2.2))
        Volatility.HIGH -> (Random.nextDouble(-4.5, 4.8))
        Volatility.CHAOTIC -> (Random.nextDouble(-8.0, 8.5))
      }

      val changeAmount = commodity.currentPrice * (deltaPercent / 100.0)
      val newPrice = max(10.0, commodity.currentPrice + changeAmount)
      val newHigh = max(commodity.high24h, newPrice)
      val newLow = min(commodity.low24h, newPrice)
      val newHistory = (commodity.priceHistory + newPrice).takeLast(30)
      val newVolume = commodity.volume24h + Random.nextLong(1, 15)

      commodity.copy(
        previousPrice = commodity.currentPrice,
        currentPrice = newPrice,
        high24h = newHigh,
        low24h = newLow,
        priceHistory = newHistory,
        volume24h = newVolume
      )
    }

    _commodities.value = updated

    // Update currently selected commodity if present
    _selectedCommodity.value?.let { current ->
      updated.find { it.id == current.id }?.let { fresh ->
        _selectedCommodity.value = fresh
      }
    }

    // Check alerts
    checkAlerts(updated)

    // Periodically fluctuate order books
    regenerateAllOrderBooks()

    // Update dynamic leaderboard rankings
    updateLeaderboard()
  }

  private fun checkAlerts(currentCommodities: List<Commodity>) {
    val currentAlerts = _alerts.value
    if (currentAlerts.isEmpty()) return

    val commodityMap = currentCommodities.associateBy { it.symbol }
    var triggeredCount = 0

    val updatedAlerts = currentAlerts.map { alert ->
      if (!alert.isTriggered) {
        val commodity = commodityMap[alert.symbol]
        if (commodity != null) {
          val isConditionMet = when (alert.condition) {
            AlertCondition.ABOVE_OR_EQUAL -> commodity.currentPrice >= alert.targetPrice
            AlertCondition.BELOW_OR_EQUAL -> commodity.currentPrice <= alert.targetPrice
          }

          if (isConditionMet) {
            triggeredCount++
            notificationHelper.sendPriceAlertNotification(
              alertId = alert.id,
              symbol = alert.symbol,
              name = commodity.name,
              targetPrice = alert.targetPrice,
              currentPrice = commodity.currentPrice,
              isAbove = alert.condition == AlertCondition.ABOVE_OR_EQUAL
            )
            appendLog("ALERT TRIGGERED: ${alert.symbol} @ %,.2f ABS".format(commodity.currentPrice))
            alert.copy(
              isTriggered = true,
              triggeredAt = System.currentTimeMillis(),
              triggeredPrice = commodity.currentPrice
            )
          } else {
            alert
          }
        } else {
          alert
        }
      } else {
        alert
      }
    }

    if (triggeredCount > 0) {
      _alerts.value = updatedAlerts
    }
  }

  fun addAlert(symbol: String, targetPrice: Double, condition: AlertCondition) {
    val commodity = _commodities.value.find { it.symbol == symbol } ?: return
    val newAlert = PriceAlert(
      id = UUID.randomUUID().toString(),
      commodityId = commodity.id,
      symbol = symbol,
      targetPrice = targetPrice,
      condition = condition
    )
    _alerts.update { listOf(newAlert) + it }
    appendLog("ALERT SET: $symbol $condition %,.2f ABS".format(targetPrice))
  }

  fun removeAlert(alertId: String) {
    _alerts.update { list -> list.filterNot { it.id == alertId } }
    appendLog("ALERT REMOVED: $alertId")
  }

  fun executeTrade(
    symbol: String,
    type: OrderType,
    kind: OrderKind,
    amount: Double,
    customPrice: Double? = null
  ): Result<String> {
    val commodity = _commodities.value.find { it.symbol == symbol }
      ?: return Result.failure(Exception("Komoditas tidak ditemukan"))

    val price = if (kind == OrderKind.MARKET) commodity.currentPrice else (customPrice ?: commodity.currentPrice)
    val totalCost = amount * price

    val currentPortfolio = _portfolio.value

    return when (type) {
      OrderType.BUY -> {
        if (currentPortfolio.cashBalance < totalCost) {
          return Result.failure(Exception("Saldo ABS tidak mencukupi (Perlu: %,.2f ABS)".format(totalCost)))
        }

        val existingHolding = currentPortfolio.holdings[symbol]
        val newUnits = (existingHolding?.units ?: 0.0) + amount
        val totalSpent = ((existingHolding?.units ?: 0.0) * (existingHolding?.avgBuyPrice ?: 0.0)) + totalCost
        val newAvgPrice = if (newUnits > 0) totalSpent / newUnits else price

        val updatedHoldings = currentPortfolio.holdings.toMutableMap().apply {
          put(symbol, PortfolioHolding(symbol, newUnits, newAvgPrice))
        }

        val trade = TradeOrder(
          id = "TRD-" + System.currentTimeMillis().toString().takeLast(6),
          symbol = symbol,
          type = type,
          kind = kind,
          price = price,
          amount = amount,
          totalCost = totalCost
        )

        _portfolio.value = currentPortfolio.copy(
          cashBalance = currentPortfolio.cashBalance - totalCost,
          holdings = updatedHoldings,
          tradeHistory = listOf(trade) + currentPortfolio.tradeHistory
        )

        appendLog("TRADE EXEC [BUY]: $amount $symbol @ %,.2f ABS".format(price))
        Result.success("Beli $amount $symbol berhasil!")
      }

      OrderType.SELL -> {
        val existingHolding = currentPortfolio.holdings[symbol]
        val currentUnits = existingHolding?.units ?: 0.0
        if (currentUnits < amount) {
          return Result.failure(Exception("Unit komoditas tidak cukup untuk dijual (Dimiliki: $currentUnits)"))
        }

        val remainingUnits = currentUnits - amount
        val updatedHoldings = currentPortfolio.holdings.toMutableMap().apply {
          if (remainingUnits > 0.0001) {
            put(symbol, existingHolding!!.copy(units = remainingUnits))
          } else {
            remove(symbol)
          }
        }

        val trade = TradeOrder(
          id = "TRD-" + System.currentTimeMillis().toString().takeLast(6),
          symbol = symbol,
          type = type,
          kind = kind,
          price = price,
          amount = amount,
          totalCost = totalCost
        )

        _portfolio.value = currentPortfolio.copy(
          cashBalance = currentPortfolio.cashBalance + totalCost,
          holdings = updatedHoldings,
          tradeHistory = listOf(trade) + currentPortfolio.tradeHistory
        )

        appendLog("TRADE EXEC [SELL]: $amount $symbol @ %,.2f ABS".format(price))
        Result.success("Jual $amount $symbol berhasil!")
      }
    }
  }

  fun resetPortfolio() {
    _portfolio.value = UserPortfolio(
      cashBalance = 15000.0,
      holdings = emptyMap(),
      tradeHistory = emptyList()
    )
    appendLog("PORTFOLIO RESET: RE-INITIALIZED TO 15,000.00 ABS")
  }

  private fun regenerateAllOrderBooks() {
    val books = mutableMapOf<String, OrderBook>()
    _commodities.value.forEach { commodity ->
      val base = commodity.currentPrice

      // Asks: slightly higher than base price
      val asks = (1..6).map { i ->
        val price = base * (1.0 + (i * 0.004) + Random.nextDouble(0.001, 0.003))
        val amount = Random.nextDouble(0.5, 12.0)
        OrderBookEntry(price = price, amount = amount, total = price * amount)
      }.sortedBy { it.price }

      // Bids: slightly lower than base price
      val bids = (1..6).map { i ->
        val price = base * (1.0 - (i * 0.004) - Random.nextDouble(0.001, 0.003))
        val amount = Random.nextDouble(0.5, 15.0)
        OrderBookEntry(price = price, amount = amount, total = price * amount)
      }.sortedByDescending { it.price }

      val maxDepth = (asks + bids).maxOfOrNull { it.total } ?: 1.0

      val normalizedAsks = asks.map { it.copy(depthPercent = (it.total / maxDepth).toFloat().coerceIn(0.1f, 1f)) }
      val normalizedBids = bids.map { it.copy(depthPercent = (it.total / maxDepth).toFloat().coerceIn(0.1f, 1f)) }

      val bestAsk = asks.firstOrNull()?.price ?: base
      val bestBid = bids.firstOrNull()?.price ?: base
      val spread = max(0.1, bestAsk - bestBid)
      val spreadPercent = (spread / base) * 100.0

      books[commodity.symbol] = OrderBook(
        symbol = commodity.symbol,
        bids = normalizedBids,
        asks = normalizedAsks,
        spread = spread,
        spreadPercent = spreadPercent
      )
    }
    _orderBooks.value = books
  }

  private fun updateLeaderboard() {
    val currentPrices = _commodities.value.associate { it.symbol to it.currentPrice }
    val userNetWorth = _portfolio.value.totalNetWorth(currentPrices)
    val userRoi = ((userNetWorth - 10000.0) / 10000.0) * 100.0

    val npcTraders = listOf(
      Trader("t1", 1, "Sultan_Halusinasi", "Paus Khayalan [LV.99]", 342.8, 4892100.0, "MMP.SNG", 284),
      Trader("t2", 2, "Bandar_Awan_99", "Meteorolog Nekat", 184.2, 2750000.0, "AWN.KUM", 195),
      Trader("t3", 3, "Dukun_Algoritma", "Hacker Mistis 1984", 95.4, 1320400.0, "GMA.GUA", 143),
      Trader("t4", 4, "Pialang_Ghaib", "Bisikan Senja", 44.1, 890200.0, "BSK.PST", 88),
      Trader("t5", 5, "Kolektor_Dejavu", "Penjelajah Garis Waktu", 28.5, 610000.0, "KNG.DJV", 56),
      Trader("t6", 6, "Investor_Melankolis", "Pecinta Gerimis", 14.8, 450000.0, "ARM.HJN", 42),
      Trader("t7", 7, "Penyimpan_Kaset", "Kolektor Analog", 6.2, 120000.0, "NST.KST", 29),
      Trader("t8", 8, "Penunda_Batas", "Master Prokrastinasi", -4.5, 45000.0, "WKT.TND", 64)
    )

    val userTrader = Trader(
      id = "user",
      rank = 9,
      username = "Anda (Trader Terminal)",
      badge = if (userRoi >= 50.0) "Spekulan Jenius" else if (userRoi >= 0.0) "Pialang Berbakat" else "Pengambil Risiko",
      roiPercent = userRoi,
      netWorth = userNetWorth,
      favoriteCommodity = _portfolio.value.holdings.keys.firstOrNull() ?: "ABS",
      tradeCount = _portfolio.value.tradeHistory.size,
      isUser = true
    )

    val combined = (npcTraders + userTrader)
      .sortedByDescending { it.netWorth }
      .mapIndexed { index, trader -> trader.copy(rank = index + 1) }

    _leaderboard.value = combined
  }

  private fun appendLog(msg: String) {
    val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
    _terminalLogs.update { logs ->
      (listOf("[$timestamp] $msg") + logs).take(40)
    }
  }

  companion object {
    private fun initialCommodities(): List<Commodity> = listOf(
      Commodity(
        id = "com_gma",
        symbol = "GMA.GUA",
        name = "Gema Gua Purba",
        unit = "dB/pantulan",
        currentPrice = 1450.0,
        previousPrice = 1420.0,
        openingPrice = 1390.0,
        high24h = 1520.0,
        low24h = 1360.0,
        volume24h = 840,
        priceHistory = listOf(1380.0, 1395.0, 1410.0, 1405.0, 1430.0, 1425.0, 1450.0),
        description = "Gema frekuensi rendah dari stalaktit kapur masa Pleistosen. Sangat dicari oleh praktisi meditasi spekulatif dan pemburu hantu akustik.",
        category = "AKUSTIK GHAIB",
        volatility = Volatility.MEDIUM
      ),
      Commodity(
        id = "com_mmp",
        symbol = "MMP.SNG",
        name = "Mimpi Siang Bolong",
        unit = "REM/menit",
        currentPrice = 3280.0,
        previousPrice = 3100.0,
        openingPrice = 2950.0,
        high24h = 3450.0,
        low24h = 2900.0,
        volume24h = 1420,
        priceHistory = listOf(2950.0, 3020.0, 3180.0, 3090.0, 3210.0, 3150.0, 3280.0),
        description = "Lamunan melayang tentang menjadi pensiunan kaya di pulau tropis. Mengalami lonjakan volatilitas ekstrem pada jam kantor pukul 13:00-15:00.",
        category = "SUBCONSCIOUS",
        volatility = Volatility.HIGH
      ),
      Commodity(
        id = "com_awn",
        symbol = "AWN.KUM",
        name = "Awan Kumulus Tk-3",
        unit = "Pascal/kg",
        currentPrice = 780.0,
        previousPrice = 795.0,
        openingPrice = 820.0,
        high24h = 840.0,
        low24h = 760.0,
        volume24h = 2100,
        priceHistory = listOf(820.0, 810.0, 805.0, 795.0, 790.0, 785.0, 780.0),
        description = "Gumpalan uap air berbentuk mirip dinosaurus atau kelinci berbulu. Komoditas favorit para penyair melankolis dan pilot pesawat kertas.",
        category = "METEOROLOGI",
        volatility = Volatility.LOW
      ),
      Commodity(
        id = "com_bsk",
        symbol = "BSK.PST",
        name = "Bisikan Perpustakaan",
        unit = "dB/bisik",
        currentPrice = 2150.0,
        previousPrice = 2130.0,
        openingPrice = 2080.0,
        high24h = 2200.0,
        low24h = 2050.0,
        volume24h = 630,
        priceHistory = listOf(2080.0, 2100.0, 2110.0, 2130.0, 2125.0, 2140.0, 2150.0),
        description = "Suara 'ssttt!' pustakawati galak yang terperangkap di rak ensiklopedia 1978. Aset lindung nilai (hedging) teraman saat pasar panik.",
        category = "AKUSTIK GHAIB",
        volatility = Volatility.LOW
      ),
      Commodity(
        id = "com_arm",
        symbol = "ARM.HJN",
        name = "Aroma Hujan Pertama",
        unit = "ozon/tetes",
        currentPrice = 5620.0,
        previousPrice = 5450.0,
        openingPrice = 5100.0,
        high24h = 5800.0,
        low24h = 5050.0,
        volume24h = 320,
        priceHistory = listOf(5100.0, 5250.0, 5380.0, 5300.0, 5490.0, 5550.0, 5620.0),
        description = "Petrichor murni dari aspal panas yang diguyur gerimis mendadak. Likuiditas super langka dengan status safe-haven legendaris.",
        category = "OLFAKTORI GHAIB",
        volatility = Volatility.MEDIUM
      ),
      Commodity(
        id = "com_kng",
        symbol = "KNG.DJV",
        name = "Kenangan Déjà Vu",
        unit = "siklus/detik",
        currentPrice = 4120.0,
        previousPrice = 4120.0,
        openingPrice = 3980.0,
        high24h = 4300.0,
        low24h = 3950.0,
        volume24h = 910,
        priceHistory = listOf(3980.0, 4050.0, 4120.0, 4080.0, 4150.0, 4100.0, 4120.0),
        description = "Perasaan tak terbantahkan bahwa Anda pernah membeli komoditas ini kemarin dengan grafik lilin yang persis sama.",
        category = "DIMENSI WAKTU",
        volatility = Volatility.MEDIUM
      ),
      Commodity(
        id = "com_nst",
        symbol = "NST.KST",
        name = "Nostalgia Kaset Kusut",
        unit = "putaran/pensil",
        currentPrice = 940.0,
        previousPrice = 960.0,
        openingPrice = 910.0,
        high24h = 990.0,
        low24h = 890.0,
        volume24h = 1850,
        priceHistory = listOf(910.0, 930.0, 955.0, 970.0, 960.0, 945.0, 940.0),
        description = "Momen menegangkan saat memutar pita kaset audio yang melilit menggunakan pensil kayu 2B. Permintaan meledak saat larut malam.",
        category = "NOSTALGIA",
        volatility = Volatility.LOW
      ),
      Commodity(
        id = "com_wkt",
        symbol = "WKT.TND",
        name = "Waktu Ditunda",
        unit = "jam/nanti",
        currentPrice = 1890.0,
        previousPrice = 1750.0,
        openingPrice = 1600.0,
        high24h = 2100.0,
        low24h = 1580.0,
        volume24h = 3400,
        priceHistory = listOf(1600.0, 1680.0, 1720.0, 1810.0, 1750.0, 1840.0, 1890.0),
        description = "Tugas yang ditunda untuk dikerjakan 'besok pagi'. Pasokan tak terbatas, namun nilainya meroket secara kacau mendekati deadline.",
        category = "PROKRASTINASI",
        volatility = Volatility.CHAOTIC
      )
    )
  }
}
