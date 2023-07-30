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

	fun copy(
		start: Long? = null,
		end: Long? = null,
		open: Double? = null,
		high: Double? = null,
		low: Double? = null,
		close: Double? = null,
		volume: Double? = null,
		trades: Int? = null,
	): Kline {
		return KlineAsset(
			start = start ?: this.start,
			end = end ?: this.end,
			open = open ?: this.open,
			high = high ?: this.high,
			low = low ?: this.low,
			close = close ?: this.close,
			volume = volume ?: this.volume,
			trades = trades ?: this.trades
		)
	}
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