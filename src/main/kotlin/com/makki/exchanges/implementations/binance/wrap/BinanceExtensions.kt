package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.abtractions.RemoteCallError
import com.makki.exchanges.implementations.binance.BinanceApi
import com.makki.exchanges.implementations.binance.models.BinanceMarketPair
import com.makki.exchanges.implementations.binance.models.BinanceMarketPairFilter
import com.makki.exchanges.implementations.binance.models.BinanceOrderFlattened
import com.makki.exchanges.logging.GlobalLogger
import com.makki.exchanges.models.*
import com.makki.exchanges.tools.findPrecision
import com.makki.exchanges.tools.inIgC
import com.makki.exchanges.wrapper.SealedApiError
import java.math.BigDecimal

internal fun BinanceOrderFlattened.toKnown(
	pair: MarketPair,
	spend: Currency,
	gain: Currency,
): KnownOrder {
	val spendOrigVolume: BigDecimal
	val spendFillVolume: BigDecimal
	val gainOrigVolume: BigDecimal
	val gainFillVolume: BigDecimal
	if (gain == pair.baseCurrency()) {
		if (this.side != OrderSide.BUY_BASE) {
			GlobalLogger.logger.printWarning("WRONG SIDE, expected BUY, got: ${this.side}")
		}
		gainFillVolume = this.baseFillVolume
		gainOrigVolume = this.baseOrigVolume
		spendFillVolume = this.quoteFillVolume
		spendOrigVolume = this.baseOrigVolume * this.price
	} else {
		if (this.side != OrderSide.SELL_BASE) {
			GlobalLogger.logger.printWarning("WRONG SIDE, expected SELL, got: ${this.side}")
		}
		spendFillVolume = this.baseFillVolume
		spendOrigVolume = this.baseOrigVolume
		gainFillVolume = this.quoteFillVolume
		gainOrigVolume = this.baseOrigVolume * this.price
	}

	return KnownOrder(
		id = this.orderId,
		pair = pair,
		state = this.state,
		spendCurrency = spend,
		gainCurrency = pair.getOpposite(spend) ?: throw IllegalStateException(),
		spendOrigVolume = spendOrigVolume,
		gainOrigVolume = gainOrigVolume,
		spendFilledVolume = spendFillVolume,
		gainFilledVolume = gainFillVolume,
		price = this.price,
	)
}

internal fun BinanceOrderFlattened.toUnknown(): UnknownOrder {
	return UnknownOrder(
		id = this.orderId,
		symbol = this.symbol,
		state = this.state,
		type = this.type,
		side = this.side,
		baseOrigVolume = this.baseOrigVolume,
		quoteOrigVolume = this.baseOrigVolume * this.price,
		baseFilledVolume = this.baseFillVolume,
		quoteFilledVolume = this.quoteFillVolume,
		price = this.price,
	)
}

internal fun binancePairToGeneric(p: BinanceMarketPair): MarketPair {
	val lotSizeFilter = p.filters.firstNotNullOfOrNull { it as? BinanceMarketPairFilter.LotSizeFilter }
	val priceFilter = p.filters.firstNotNullOfOrNull { it as? BinanceMarketPairFilter.PriceFilter }
	val notionFilter = p.filters.firstNotNullOfOrNull { it as? BinanceMarketPairFilter.NotionalFilter }

	// find '1' in 10.000 or 0.0001
	val filterBasePrecision = lotSizeFilter?.stepSize?.findPrecision()
	val filterQuotePrecision = priceFilter?.tickSize?.findPrecision()

	return DetailedMarketPair(
		base = p.base,
		quote = p.quote,
		basePrecision = filterBasePrecision ?: p.basePrecision,
		quotePrecision = filterQuotePrecision ?: p.quotePrecision,
		minBaseVolume = lotSizeFilter?.minQty ?: 0.0,
		minQuoteVolume = notionFilter?.minNotional ?: 0.0,
		minBasePrice = priceFilter?.minPrice ?: 0.0,
		takeRatio = 0.9985, // hardcoded
		makerRatio = 0.9985 // hardcoded
	)
}

internal fun RemoteCallError<BinanceApi.BinanceError>.toSealedApiErrorExt(): SealedApiError {
	return when (this) {
		is RemoteCallError.ApiError -> this.error.toSealedApiErrorExt()
		is RemoteCallError.HttpError -> this.httpToSealedErrorExt()
		is RemoteCallError.ParseError -> SealedApiError.Unexpected(this.exception.stackTraceToString())
		is RemoteCallError.ConnectionError -> SealedApiError.ConnectionError(this.exception.stackTraceToString())
	}
}

internal fun BinanceApi.BinanceError.toSealedApiErrorExt(): SealedApiError {
	val byCode: SealedApiError? = when (this.code) {
		-1000, -3000 -> SealedApiError.InternalExchangeError
		-1001, -1016 -> SealedApiError.ExchangeIsOutOfService
		-1002, -1022, -1099, -2014, -4056, -4057, -4080, -3001 -> SealedApiError.InvalidAuth
		-1003, -1004, -1008, -1015 -> SealedApiError.RateLimited
		-1021 -> SealedApiError.NonceRaceCondition

		-1100, -1101, -1102, -1103, -1105, -1106, -1111,
		-1114, -1115, -1116, -1117, -1120, -1131,
		-1151, -2016,
		-> SealedApiError.BadRequestException(this.msg)

		-1112 -> SealedApiError.Order.PriceFillMiss
		-1109 -> SealedApiError.Banned
		-1108, -1121, -1110, -4141, -3004 -> SealedApiError.MarketBlocked
		-2010 -> SealedApiError.Order.OrderReject
		-2011 -> SealedApiError.Order.CancelReject
		-2013 -> SealedApiError.Order.OrderNotFound // 2011 is for cancel, 2013 get order
		-2018 -> SealedApiError.Order.InsufficientBalance
		-2020 -> SealedApiError.Order.PriceFillMiss
		else -> null
	}
	if (byCode != null) return byCode

	val byMsg = when {
		msg.inIgC("Invalid API-key, IP, or permissions for action") -> SealedApiError.InvalidAuth
		msg.inIgC("Filter failure") || msg.inIgC("LOT_SIZE") || msg.inIgC("MIN_NOTIONAL") || msg.inIgC("Invalid quantity") -> {
			SealedApiError.Order.FilterFailure(msg)
		}

		else -> null
	}

	if (byMsg != null) return byMsg

	return SealedApiError.Unexpected("Code: ${code}, msg: $msg")
}

internal fun RemoteCallError.HttpError<*>.httpToSealedErrorExt(): SealedApiError {
	return when (this.code) {
		400 -> SealedApiError.BadRequestException(this.msg)
		401 -> SealedApiError.InvalidAuth
		// 418 is a binance iAmATeapot reserved for a temporary ban for days
		418, 403 -> SealedApiError.Banned
		500 -> SealedApiError.InternalExchangeError
		502, 503, 504 -> SealedApiError.ExchangeIsOutOfService
		429 -> SealedApiError.RateLimited
		else -> SealedApiError.Unexpected("Http error: $this")
	}
}