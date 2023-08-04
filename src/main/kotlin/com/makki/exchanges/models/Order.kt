package com.makki.exchanges.models

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
@JvmInline
value class OrderId(val id: String) {
	companion object {
		fun fromInt(value: Int): OrderId {
			return OrderId(value.toString())
		}
	}
}

enum class OrderState {
	OPEN, CLOSED, UNKNOWN
}

enum class OrderType {
	LIMIT, UNKNOWN
}

enum class OrderSide {
	SELL_BASE,
	BUY_BASE,
	UNKNOWN,
}

sealed interface Order {
	val id: OrderId
	val state: OrderState

	fun isOpen() = state == OrderState.OPEN
	fun isClosed() = state == OrderState.CLOSED
}

// for new or pending orders
data class KnownOrder(
	override val id: OrderId,
	override val state: OrderState,
	val pair: MarketPair,
	val spendCurrency: Currency,
	val gainCurrency: Currency,
	val spendOrigVolume: BigDecimal, // full volume of order
	val gainOrigVolume: BigDecimal, // full volume of order
	val price: BigDecimal,
	val spendFilledVolume: BigDecimal,
	val gainFilledVolume: BigDecimal,
) : Order {
	fun isFilled() = spendFilledVolume == BigDecimal.ZERO || gainFilledVolume == BigDecimal.ZERO
	fun shortFormat() = "${pair.prettyName()}:${id.id}"
}

data class UnknownOrder(
	override val id: OrderId,
	override val state: OrderState,
	val symbol: String,
	val type: OrderType,
	val side: OrderSide,
	val baseOrigVolume: BigDecimal, // full volume of order
	val quoteOrigVolume: BigDecimal, // full volume of order
	val price: BigDecimal,
	val baseFilledVolume: BigDecimal,
	val quoteFilledVolume: BigDecimal,
) : Order
