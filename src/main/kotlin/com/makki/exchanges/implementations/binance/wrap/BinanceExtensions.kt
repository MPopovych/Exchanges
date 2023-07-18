package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.abtractions.RestResult
import com.makki.exchanges.implementations.binance.BinanceApi
import com.makki.exchanges.implementations.binance.models.BinanceKline
import com.makki.exchanges.implementations.binance.models.BinanceMarketPair
import com.makki.exchanges.implementations.binance.models.BinanceMarketPairFilter
import com.makki.exchanges.models.DetailedMarketPair
import com.makki.exchanges.tools.inIgC
import com.makki.exchanges.tools.produceError
import com.makki.exchanges.wrapper.SealedApiError
import com.makki.exchanges.models.KlineEntry
import com.makki.exchanges.models.MarketPair
import com.makki.exchanges.tools.findPrecision

internal fun binancePairToGeneric(p: BinanceMarketPair): MarketPair {
	val lotSizeFilter = p.filters.firstNotNullOfOrNull { it as? BinanceMarketPairFilter.LotSizeFilter }
	val priceFilter = p.filters.firstNotNullOfOrNull { it as? BinanceMarketPairFilter.PriceFilter }

	// find '1' in 10.000 or 0.0001
	val filterBasePrecision = lotSizeFilter?.stepSize?.findPrecision()
	val filterQuotePrecision = priceFilter?.tickSize?.findPrecision()

	return DetailedMarketPair(
		base = p.base,
		quote = p.quote,
		basePrecision = filterBasePrecision ?: p.basePrecision,
		quotePrecision = filterQuotePrecision ?: p.quotePrecision,
		minBaseVolume = lotSizeFilter?.minQty ?: 0.0,
		minBasePrice = priceFilter?.minPrice ?: 0.0,
		takeRatio = 0.999, // hardcoded
		makerRatio = 0.999 // hardcoded
	)
}



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
		msg.inIgC("Invalid API-key, IP, or permissions for action") -> SealedApiError.InvalidAuth
		msg.inIgC("LOT_SIZE") || msg.inIgC("MIN_NOTIONAL") || msg.inIgC("Invalid quantity") -> {
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