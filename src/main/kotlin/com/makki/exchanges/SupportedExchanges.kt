package com.makki.exchanges

import com.makki.exchanges.abtractions.KlineInterval
import com.makki.exchanges.implementations.binance.BinanceApi
import com.makki.exchanges.implementations.binance.models.BinanceKlineInterval
import com.makki.exchanges.implementations.binance.wrap.BinanceWrap
import com.makki.exchanges.tools.eqIgC
import com.makki.exchanges.wrapper.ApiWrapper
import com.makki.exchanges.wrapper.SafeGuardConfig

/**
 * Utility interface for defining
 */
interface ExchangeDefinition {
	val exchangeName: String
	val intervals: List<KlineInterval>
	val wrap: ApiWrapper
}

sealed interface SupportedExchanges : ExchangeDefinition {

	// for unauthorized (public only) access
	object BinancePublic : SupportedExchanges {
		val api = BinanceApi()
		override val wrap: BinanceWrap = BinanceWrap(api)
		override val exchangeName: String = "binance"
		override val intervals: List<KlineInterval> = BinanceKlineInterval.values().toList()
	}

	// for authorized access and mocks
	class Binance(override val wrap: BinanceWrap) : SupportedExchanges {
		override val exchangeName: String = "binance"
		override val intervals: List<KlineInterval> = BinanceKlineInterval.values().toList()
	}

	class BinanceTestNet(apiKey: String, secret: String, safeGuardConfig: SafeGuardConfig = SafeGuardConfig()) : SupportedExchanges {
		val api = BinanceApi(
			pubKey = apiKey,
			secretKey = secret,
			baseUrl = BinanceApi.TEST_SPOT_API_URL
		)
		override val wrap: BinanceWrap = BinanceWrap(api, safeGuardConfig)
		override val exchangeName: String = "binance"
		override val intervals: List<KlineInterval> = BinanceKlineInterval.values().toList()
	}

	fun findApiInterval(interval: String): KlineInterval? {
		return this.intervals.find { interval.eqIgC(it.apiCode) }
	}
}