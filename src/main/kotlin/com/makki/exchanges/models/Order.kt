package com.makki.exchanges.models

import com.makki.exchanges.tools.trimStr
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

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
	val created = System.currentTimeMillis()

	fun isFilled() = spendFilledVolume.signum() > 0 || gainFilledVolume.signum() > 0
	fun fillRatio() = spendFilledVolume / spendOrigVolume
	fun fillRatioString() = (fillRatio() * BigDecimal.valueOf(100))
		.setScale(2, RoundingMode.HALF_EVEN)
		.trimStr()

	fun shortFormat() = "${pair.prettyName()}.${id.id}"
	fun longFormat() = "${pair.prettyName()}.${id.id}.price(${price.trimStr()})"
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
