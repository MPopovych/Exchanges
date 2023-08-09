@file:UseSerializers(BigDecimalSerializer::class, InstantSerializer::class)

package com.makki.exchanges.models

import com.makki.exchanges.abtractions.StateObservable
import com.makki.exchanges.common.serializers.BigDecimalSerializer
import com.makki.exchanges.common.serializers.InstantSerializer
import com.makki.exchanges.tools.StateTree
import com.makki.exchanges.tools.trimStr
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
	operator fun get(currency: Currency) = map[currency.lowName()]

	fun iterator() = map.values.iterator()
	fun list() = map.values.toList()
	fun size() = map.size
	fun nonZero() = BalanceBook(list().filter { entry -> entry.total.signum() != 0 || entry.frozen.signum() != 0 })
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
) : StateObservable {

	override fun stateTree(): StateTree = StateTree()
		.track("name") { name }
		.track("available") { available.trimStr() }
		.track("frozen") { frozen.trimStr() }
		.track("total") { total.trimStr() }

}