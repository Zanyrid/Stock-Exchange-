package com.example.model

enum class AlertCondition {
  ABOVE_OR_EQUAL, // >= target price
  BELOW_OR_EQUAL  // <= target price
}

data class PriceAlert(
  val id: String,
  val commodityId: String,
  val symbol: String,
  val targetPrice: Double,
  val condition: AlertCondition,
  val isTriggered: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
  val triggeredAt: Long? = null,
  val triggeredPrice: Double? = null
)
