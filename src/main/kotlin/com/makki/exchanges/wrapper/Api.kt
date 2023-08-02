package com.makki.exchanges.wrapper

import com.makki.exchanges.abtractions.Frame
import com.makki.exchanges.abtractions.KlineInterval
import com.makki.exchanges.common.Result
import com.makki.exchanges.common.onError
import com.makki.exchanges.models.BalanceBook
import com.makki.exchanges.models.Kline
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

/**
 * Wrap a method and intercepts the error, pushes it to the implemented method
 */
interface WrapTraitErrorStream {
	suspend fun trackErrors(): Flow<SealedApiError>
	suspend fun notifyError(result: SealedApiError)

	suspend fun <T> WSWrapper.notify(block: suspend () -> Result<T, SealedApiError>): Result<T, SealedApiError> {
		return block().onError { e ->
			notifyError(e)
		}
	}
}

interface WrapTraitApiKline {
	suspend fun klineData(
		market: String,
		interval: String,
		limit: Int = 500,
		range: LongRange? = null,
	): Result<List<Kline>, SealedApiError>
}

interface WrapTraitApiMarketInfo {
	suspend fun marketInfo(): Result<List<MarketPair>, SealedApiError>
}

interface WrapTraitApiBalance {
	suspend fun balance(): Result<BalanceBook, SealedApiError>
}

interface WrapTraitSocketKline : WSWrapper {
	suspend fun trackKline(
		market: String,
		interval: String,
	): Flow<Frame<Kline>>

	suspend fun trackKline(
		market: String,
		interval: KlineInterval,
	): Flow<Frame<Kline>> = trackKline(market, interval.apiCode)
}

// region casts

@Throws
fun ApiWrapper.requireBalance(): WrapTraitApiBalance {
	return this as? WrapTraitApiBalance ?: throw NotImplementedError("Failed requirement for balance api")
}

@Throws
fun ApiWrapper.requireKlineApi(): WrapTraitApiKline {
	return this as? WrapTraitApiKline ?: throw NotImplementedError("Failed requirement for Kline api")
}

@Throws
fun ApiWrapper.requireKlineWS(): WrapTraitSocketKline {
	return this as? WrapTraitSocketKline ?: throw NotImplementedError("Failed requirement for Kline ws")
}

// endregion