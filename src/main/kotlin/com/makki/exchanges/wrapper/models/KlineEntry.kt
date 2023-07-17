package com.makki.exchanges.wrapper.models

import kotlinx.serialization.Serializable

@Serializable
data class KlineEntry(
	val start: Long,
	val end: Long,
	val open: Double,
	val high: Double,
	val low: Double,
	val close: Double,
	val volume: Double,
	val trades: Int,
)