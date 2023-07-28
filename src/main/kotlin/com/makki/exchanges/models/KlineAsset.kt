package com.makki.exchanges.models

import kotlinx.serialization.Serializable


interface Kline {
	val start: Long
	val end: Long
	val open: Double
	val high: Double
	val low: Double
	val close: Double
	val volume: Double
	val trades: Int
}

@Serializable
data class KlineAsset(
	override val start: Long,
	override val end: Long,
	override val open: Double,
	override val high: Double,
	override val low: Double,
	override val close: Double,
	override val volume: Double,
	override val trades: Int,
) : Kline