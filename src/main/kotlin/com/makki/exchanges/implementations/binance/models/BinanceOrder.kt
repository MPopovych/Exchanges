@file:UseSerializers(BigDecimalSerializer::class)
@file:Suppress("ClassName")

package com.makki.exchanges.implementations.binance.models

import com.makki.exchanges.common.serializers.BigDecimalSerializer
import com.makki.exchanges.models.OrderId
import com.makki.exchanges.models.OrderSide
import com.makki.exchanges.models.OrderState
import com.makki.exchanges.models.OrderType
import com.makki.exchanges.tools.eqIgC
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigDecimal

@Suppress("unused")
enum class BinanceOrderStates(val value: String) {
	New("NEW"), // The order has been accepted by the engine.
	PartialFill("PARTIALLY_FILLED"), // A part of the order has been filled.
	Filled("FILLED"), // The order has been completed.
	Canceled("CANCELED"), // The order has been canceled by the user.
	Rejected("REJECTED"), // The order was not accepted by the engine and not processed.
	Expired("EXPIRED"), // The order was canceled according to the order type's rules
	Prevented("EXPIRED_IN_MATCH"); // The order was canceled by the exchange due to STP trigger.

	companion object {
		fun byStrValue(value: String): OrderState {
			return values().find { enum -> enum.value.eqIgC(value) }?.toOrderState() ?: OrderState.UNKNOWN
		}
	}

	fun toOrderState(): OrderState {
		return when (this) {
			New, PartialFill -> OrderState.OPEN
			else -> OrderState.CLOSED
		}
	}
}

@Serializable
class BinanceOrder_RESULT(
	val symbol: String,
	val orderId: Int,
	val price: BigDecimal,
	val origQty: BigDecimal, // base qty
	val executedQty: BigDecimal, // base qty
	val cummulativeQuoteQty: BigDecimal, // quote qty
	val status: String,
	val type: String,
	val side: String,
)

class BinanceOrderFlattened(
	val orderId: OrderId,
	val symbol: String,
	val price: BigDecimal,
	val baseOrigVolume: BigDecimal,
	val baseFillVolume: BigDecimal,
	val quoteFillVolume: BigDecimal,
	val state: OrderState,
	val type: OrderType,
	val side: OrderSide,
) {

	init {
		require(price >= BigDecimal.ZERO) { this.toString() }
		require(baseOrigVolume >= BigDecimal.ZERO) { this.toString() }
		require(baseFillVolume >= BigDecimal.ZERO) { this.toString() }
		require(quoteFillVolume >= BigDecimal.ZERO) { this.toString() }
	}

	companion object {
		fun from(orderFull: BinanceOrder_RESULT): BinanceOrderFlattened {
			val orderId = OrderId.fromInt(orderFull.orderId)
			val state = BinanceOrderStates.byStrValue(orderFull.status)

			val side = when (orderFull.side) {
				"SELL" -> OrderSide.SELL_BASE
				"BUY" -> OrderSide.BUY_BASE
				else -> OrderSide.UNKNOWN
			}
			val type = when(orderFull.type) {
				"LIMIT" -> OrderType.LIMIT
				else -> OrderType.UNKNOWN
			}

			return BinanceOrderFlattened(
				orderId = orderId,
				symbol = orderFull.symbol,
				price = orderFull.price,
				baseOrigVolume = orderFull.origQty,
				baseFillVolume = orderFull.executedQty,
				quoteFillVolume = orderFull.cummulativeQuoteQty,
				state = state,
				side = side,
				type = type
			)
		}
	}
}