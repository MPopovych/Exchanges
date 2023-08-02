package com.makki.exchanges.models

import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.TemporalAmount

class BalanceBook(
	entries: List<BalanceEntry>,
) {
	private val map = entries.associateBy { it.baseName.lowercase() }
	private val created = Instant.now()

	operator fun get(name: String) = map[name.lowercase()]

	fun iterator() = map.values.iterator()

	fun isFresh(allowed: TemporalAmount): Boolean {
		return created.plus(allowed) >= Instant.now()
	}
}

class BalanceEntry(
	baseName: String,
	val available: BigDecimal,
	val frozen: BigDecimal,
) {
	init {
		require(baseName.isNotBlank())
	}

	val baseName: String = baseName.lowercase()

	val total: BigDecimal
		get() = available + frozen
}