package com.example.model

data class Trader(
  val id: String,
  val rank: Int,
  val username: String,
  val badge: String,
  val roiPercent: Double,
  val netWorth: Double,
  val favoriteCommodity: String,
  val tradeCount: Int,
  val isUser: Boolean = false
)
