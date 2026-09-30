package com.example.model

enum class Volatility {
  LOW,
  MEDIUM,
  HIGH,
  CHAOTIC
}

data class Commodity(
  val id: String,
  val symbol: String,
  val name: String,
  val unit: String,
  val currentPrice: Double,
  val previousPrice: Double,
  val openingPrice: Double,
  val high24h: Double,
  val low24h: Double,
  val volume24h: Long,
  val priceHistory: List<Double>,
  val description: String,
  val category: String,
  val volatility: Volatility = Volatility.MEDIUM
) {
  val change24h: Double
    get() = currentPrice - openingPrice

  val change24hPercent: Double
    get() = if (openingPrice > 0.0) ((currentPrice - openingPrice) / openingPrice) * 100.0 else 0.0

  val isPositive: Boolean
    get() = change24hPercent >= 0.0
}
