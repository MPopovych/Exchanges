package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.abtractions.RestResult
import com.makki.exchanges.implementations.binance.BinanceApi
import com.makki.exchanges.implementations.binance.models.BinanceKline
import com.makki.exchanges.logging.printLogRed
import com.makki.exchanges.tools.inC
import com.makki.exchanges.tools.produceError
import com.makki.exchanges.wrapper.SealedApiError
import com.makki.exchanges.wrapper.models.KlineEntry

internal fun binanceKlineToGeneric(k: BinanceKline): KlineEntry {
	return KlineEntry(
		start = k.start,
		end = k.end,
		open = k.open,
		high = k.high,
		low = k.low,
		close = k.close,
		volume = k.volume,
		trades = k.trades,
	)
}

/**
 * Mapping from binance error to enum class
 * TODO: Map onto described constants
 */
internal fun BinanceApi.BinanceError.toSealedApiErrorExt(
	market: String? = null,
	orderId: String? = null,
): SealedApiError {
	val byCode: SealedApiError? = when (this.code) {
		-1011, -1022, -2014, -4056, -4057, -4080 -> SealedApiError.InvalidAuth
		-1000 -> SealedApiError.InternalExchangeError
		-1001 -> SealedApiError.ExchangeIsOutOfService
		-1015 -> SealedApiError.RateLimited(false)
		-1021 -> SealedApiError.NonceRaceCondition

		-1109 -> SealedApiError.Banned

		-1108, -1110, -1121, -4141 -> SealedApiError.MarketBlocked(market ?: produceError())

		-2013 -> SealedApiError.Order.OrderNotFound(orderId ?: produceError())
		-2018 -> SealedApiError.Order.InsufficientBalance(orderId ?: produceError())
		-2020 -> SealedApiError.Order.PriceFillMiss(orderId ?: produceError())
		else -> null
	}
	if (byCode != null) return byCode

	val byMsg = when {
		msg.inC("Invalid API-key, IP, or permissions for action") -> SealedApiError.InvalidAuth
		msg.inC("LOT_SIZE") || msg.inC("MIN_NOTIONAL") || msg.inC("Invalid quantity") -> {
			SealedApiError.Order.InsufficientBalance(orderId ?: produceError())
		}
		else -> null
	}

	if (byMsg != null) return byMsg

	return SealedApiError.Unexpected("Code: ${code}, msg: $msg")
}

internal fun RestResult.HttpError<*, *>.toSealedErrorExt(): SealedApiError? {
	return when (code) {
		418 -> SealedApiError.Banned
		else -> null
	}
}