package com.example.model

data class OrderBookEntry(
  val price: Double,
  val amount: Double,
  val total: Double,
  val depthPercent: Float = 0f
)

data class OrderBook(
  val symbol: String,
  val bids: List<OrderBookEntry>, // Buy orders (green)
  val asks: List<OrderBookEntry>, // Sell orders (amber/red)
  val spread: Double,
  val spreadPercent: Double
)

enum class OrderType {
  BUY,
  SELL
}

enum class OrderKind {
  LIMIT,
  MARKET
}

data class TradeOrder(
  val id: String,
  val symbol: String,
  val type: OrderType,
  val kind: OrderKind,
  val price: Double,
  val amount: Double,
  val totalCost: Double,
  val timestamp: Long = System.currentTimeMillis(),
  val status: String = "EXECUTED"
)
