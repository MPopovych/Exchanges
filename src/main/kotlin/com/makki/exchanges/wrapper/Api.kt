package com.makki.exchanges.wrapper

import com.makki.exchanges.abtractions.KlineInterval
import com.makki.exchanges.abtractions.RestResult
import com.makki.exchanges.models.KlineEntry
import com.makki.exchanges.models.MarketPair
import kotlinx.coroutines.flow.Flow

/**
 * Api wrapper is a trait based object with optional functionalities.
 * Aka Rust style, implementations are added onto the wrapper.
 * Projects using this should check on startup for functionalities
 * and throw if a selected exchanges is missing core features.
 *
 * @property start is for starting sockets, loading stuff, etc
 * @property stop is for stopping all pending sockets, may be used to reset state
 */
interface ApiWrapper {
	fun start() {}
	fun stop() {}
	fun readApiName(marketPair: MarketPair): String
}

interface WrapTraitErrorStream {
	suspend fun trackErrors(): Flow<RestResult<*, SealedApiError>>
}

interface WrapTraitApiKline {
	suspend fun klineData(
		market: String,
		interval: String,
		limit: Int = 500,
		range: LongRange? = null,
	): RestResult<List<KlineEntry>, SealedApiError>
}

interface WrapTraitApiMarketInfo {
	suspend fun marketInfo(): RestResult<List<MarketPair>, SealedApiError>
}

interface WrapTraitSocketKline {
	suspend fun trackKline(
		market: String,
		interval: String,
	): Flow<KlineEntry>

	suspend fun trackKline(
		market: String,
		interval: KlineInterval,
	): Flow<KlineEntry> = trackKline(market, interval.apiCode)
}

// region casts

@Throws
fun ApiWrapper.requireKlineApi(): WrapTraitApiKline {
	return this as? WrapTraitApiKline ?: throw NotImplementedError("Failed requirement for Kline api")
}

// endregion