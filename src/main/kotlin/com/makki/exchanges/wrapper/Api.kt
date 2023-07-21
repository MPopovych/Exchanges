package com.makki.exchanges.wrapper

import com.makki.exchanges.abtractions.Frame
import com.makki.exchanges.abtractions.KlineInterval
import com.makki.exchanges.common.Result
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

interface WSWrapper {
	fun readWSName(marketPair: MarketPair): String
}

interface WrapTraitErrorStream {
	suspend fun trackErrors(): Flow<SealedApiError>
}

interface WrapTraitApiKline {
	suspend fun klineData(
		market: String,
		interval: String,
		limit: Int = 500,
		range: LongRange? = null,
	): Result<List<KlineEntry>, SealedApiError>
}

interface WrapTraitApiMarketInfo {
	suspend fun marketInfo(): Result<List<MarketPair>, SealedApiError>
}

interface WrapTraitSocketKline : WSWrapper {
	suspend fun trackKline(
		market: String,
		interval: String,
	): Flow<Frame<KlineEntry>>

	suspend fun trackKline(
		market: String,
		interval: KlineInterval,
	): Flow<Frame<KlineEntry>> = trackKline(market, interval.apiCode)
}

// region casts

@Throws
fun ApiWrapper.requireKlineApi(): WrapTraitApiKline {
	return this as? WrapTraitApiKline ?: throw NotImplementedError("Failed requirement for Kline api")
}

@Throws
fun ApiWrapper.requireKlineWS(): WrapTraitSocketKline {
	return this as? WrapTraitSocketKline ?: throw NotImplementedError("Failed requirement for Kline ws")
}

// endregion