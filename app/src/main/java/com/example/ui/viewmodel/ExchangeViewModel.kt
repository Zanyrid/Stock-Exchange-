package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AbsurdExchangeRepository
import com.example.data.ExchangeSettings
import com.example.data.NotificationHelper
import com.example.model.AlertCondition
import com.example.model.Commodity
import com.example.model.OrderBook
import com.example.model.OrderKind
import com.example.model.OrderType
import com.example.model.PriceAlert
import com.example.model.Trader
import com.example.model.UserPortfolio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExchangeViewModel(application: Application) : AndroidViewModel(application) {

  private val notificationHelper = NotificationHelper(application.applicationContext)
  private val repository = AbsurdExchangeRepository(notificationHelper, viewModelScope)

  val commodities: StateFlow<List<Commodity>> = repository.commodities
  val selectedCommodity: StateFlow<Commodity?> = repository.selectedCommodity
  val orderBooks: StateFlow<Map<String, OrderBook>> = repository.orderBooks
  val portfolio: StateFlow<UserPortfolio> = repository.portfolio
  val leaderboard: StateFlow<List<Trader>> = repository.leaderboard
  val alerts: StateFlow<List<PriceAlert>> = repository.alerts
  val settings: StateFlow<ExchangeSettings> = repository.settings
  val terminalLogs: StateFlow<List<String>> = repository.terminalLogs
  val connectionStatus: StateFlow<String> = repository.connectionStatus

  private val _tradeMessage = MutableStateFlow<String?>(null)
  val tradeMessage: StateFlow<String?> = _tradeMessage.asStateFlow()

  private val _testConnectionResult = MutableStateFlow<Pair<Boolean, String>?>(null)
  val testConnectionResult: StateFlow<Pair<Boolean, String>?> = _testConnectionResult.asStateFlow()

  private val _isTestingConnection = MutableStateFlow(false)
  val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

  fun selectCommodity(commodity: Commodity) {
    repository.selectCommodity(commodity)
  }

  fun executeTrade(
    symbol: String,
    type: OrderType,
    kind: OrderKind,
    amount: Double,
    customPrice: Double? = null
  ) {
    val result = repository.executeTrade(symbol, type, kind, amount, customPrice)
    result.fold(
      onSuccess = { _tradeMessage.value = "SUKSES: $it" },
      onFailure = { _tradeMessage.value = "GAGAL: ${it.message}" }
    )
  }

  fun clearTradeMessage() {
    _tradeMessage.value = null
  }

  fun addPriceAlert(symbol: String, targetPrice: Double, condition: AlertCondition) {
    repository.addAlert(symbol, targetPrice, condition)
  }

  fun removePriceAlert(alertId: String) {
    repository.removeAlert(alertId)
  }

  fun updateSettings(newSettings: ExchangeSettings) {
    repository.updateSettings(newSettings)
  }

  fun testConnection(url: String) {
    viewModelScope.launch {
      _isTestingConnection.value = true
      _testConnectionResult.value = null
      val res = repository.testConnection(url)
      _testConnectionResult.value = res
      _isTestingConnection.value = false
    }
  }

  fun resetPortfolio() {
    repository.resetPortfolio()
  }
}
