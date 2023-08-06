@file:UseSerializers(BigDecimalSerializer::class, InstantSerializer::class)

package com.makki.exchanges.models

import com.makki.exchanges.common.serializers.BigDecimalSerializer
import com.makki.exchanges.common.serializers.InstantSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.TemporalAmount

class BalanceBook(
	entries: List<BalanceEntry>,
) {
	private val map = entries.associateBy { it.name.lowercase() }
	val created = Instant.now()

	operator fun get(name: String) = map[name.lowercase()]
	operator fun get(currency: Currency) = map[currency.name.lowercase()]

	fun iterator() = map.values.iterator()
	fun size() = map.size
	fun toSerializableMap() = map.mapValues { it.value.toSerializable() }

	fun isFresh(allowed: TemporalAmount): Boolean {
		return created.plus(allowed) >= Instant.now()
	}
}

class BalanceEntry(
	name: String,
	val available: BigDecimal,
	val frozen: BigDecimal,
) {
	init {
		require(name.isNotBlank())
	}

	val name: String = name.lowercase()

	val total: BigDecimal
		get() = available + frozen

	fun toSerializable() = SerializableBalanceEntry(
		name, available.stripTrailingZeros(), frozen.stripTrailingZeros(), total.stripTrailingZeros()
	)
}

@Serializable
data class SerializableBalanceEntry(
	val name: String,
	val available: BigDecimal,
	val frozen: BigDecimal,
	val total: BigDecimal,
)