package com.example.model

data class PortfolioHolding(
  val symbol: String,
  val units: Double,
  val avgBuyPrice: Double
) {
  fun currentValue(currentPrice: Double): Double = units * currentPrice
  fun profitLoss(currentPrice: Double): Double = currentValue(currentPrice) - (units * avgBuyPrice)
  fun profitLossPercent(currentPrice: Double): Double =
    if (avgBuyPrice > 0.0) ((currentPrice - avgBuyPrice) / avgBuyPrice) * 100.0 else 0.0
}

data class UserPortfolio(
  val cashBalance: Double = 10000.0, // Absurd Bucks (ABS)
  val holdings: Map<String, PortfolioHolding> = emptyMap(),
  val tradeHistory: List<TradeOrder> = emptyList()
) {
  fun totalNetWorth(currentPrices: Map<String, Double>): Double {
    val holdingsValue = holdings.values.sumOf { holding ->
      val price = currentPrices[holding.symbol] ?: holding.avgBuyPrice
      holding.currentValue(price)
    }
    return cashBalance + holdingsValue
  }
}
