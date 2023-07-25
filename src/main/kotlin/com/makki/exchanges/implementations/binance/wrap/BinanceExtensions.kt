package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.abtractions.RemoteCallError
import com.makki.exchanges.implementations.binance.BinanceApi
import com.makki.exchanges.implementations.binance.models.BinanceKline
import com.makki.exchanges.implementations.binance.models.BinanceMarketPair
import com.makki.exchanges.implementations.binance.models.BinanceMarketPairFilter
import com.makki.exchanges.implementations.binance.models.BinanceSocketKlineAsset
import com.makki.exchanges.models.DetailedMarketPair
import com.makki.exchanges.models.Kline
import com.makki.exchanges.models.KlineAsset
import com.makki.exchanges.models.MarketPair
import com.makki.exchanges.tools.findPrecision
import com.makki.exchanges.tools.inIgC
import com.makki.exchanges.wrapper.SealedApiError

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

internal fun RemoteCallError<BinanceApi.BinanceError>.toSealedApiErrorExt(): SealedApiError {
	return when (this) {
		is RemoteCallError.ApiError -> this.error.toSealedApiErrorExt()
		is RemoteCallError.HttpError -> this.toSealedErrorExt()
		is RemoteCallError.ParseError -> SealedApiError.Unexpected(this.exception.stackTraceToString())
		is RemoteCallError.ConnectionError -> SealedApiError.ConnectionError(this.exception.stackTraceToString())
	}
}

/**
 * Mapping from binance error to enum class
 * TODO: Map onto described constants
 */
internal fun BinanceApi.BinanceError.toSealedApiErrorExt(): SealedApiError {
	val byCode: SealedApiError? = when (this.code) {
		-1011, -1022, -2014, -4056, -4057, -4080 -> SealedApiError.InvalidAuth
		-1000 -> SealedApiError.InternalExchangeError
		-1001 -> SealedApiError.ExchangeIsOutOfService
		-1015 -> SealedApiError.RateLimited
		-1021 -> SealedApiError.NonceRaceCondition
		-1109 -> SealedApiError.Banned
		-1108, -1110, -1121, -4141 -> SealedApiError.MarketBlocked
		-2013 -> SealedApiError.Order.OrderNotFound
		-2018 -> SealedApiError.Order.InsufficientBalance
		-2020 -> SealedApiError.Order.PriceFillMiss
		else -> null
	}
	if (byCode != null) return byCode

	val byMsg = when {
		msg.inIgC("Invalid API-key, IP, or permissions for action") -> SealedApiError.InvalidAuth
		msg.inIgC("LOT_SIZE") || msg.inIgC("MIN_NOTIONAL") || msg.inIgC("Invalid quantity") -> {
			SealedApiError.Order.VolumeLessThanMinimum
		}

		else -> null
	}

	if (byMsg != null) return byMsg

	return SealedApiError.Unexpected("Code: ${code}, msg: $msg")
}

internal fun RemoteCallError.HttpError<*>.toSealedErrorExt(): SealedApiError {
	return when (this.code) {
		401 -> SealedApiError.InvalidAuth
		// 418 is a binance iAmATeapot reserved for a temporary ban for days
		418, 403 -> SealedApiError.Banned
		500 -> SealedApiError.InternalExchangeError
		502, 503, 504 -> SealedApiError.ExchangeIsOutOfService
		429 -> SealedApiError.RateLimited
		else -> SealedApiError.Unexpected("Http error: ${this.code}")
	}
}