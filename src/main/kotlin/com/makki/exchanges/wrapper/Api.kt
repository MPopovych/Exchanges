package com.makki.exchanges.wrapper

import com.makki.exchanges.abtractions.Frame
import com.makki.exchanges.abtractions.KlineInterval
import com.makki.exchanges.common.Result
import com.makki.exchanges.common.onError
import com.makki.exchanges.common.wrapError
import com.makki.exchanges.models.*
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

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
		return try {
			block().onError { e ->
				notifyError(e)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			return SealedApiError.Unexpected(e.message ?: e.localizedMessage).wrapError()
		}
	}
}

interface WrapTraitApiKline : ApiWrapper {
	suspend fun klineData(
		market: String,
		interval: String,
		limit: Int = 500,
		range: LongRange? = null,
	): Result<List<Kline>, SealedApiError>

	suspend fun klineData(
		market: MarketPair,
		interval: KlineInterval,
		limit: Int = 500,
		range: LongRange? = null,
	): Result<List<Kline>, SealedApiError> = klineData(readApiName(market), interval.apiCode, limit, range)
}

interface WrapTraitApiMarketInfo {
	suspend fun marketInfo(): Result<List<MarketPair>, SealedApiError>
}

interface WrapTraitApiBalance {
	suspend fun balance(): Result<BalanceBook, SealedApiError>
}

interface WrapTraitApiOrder {

	suspend fun allOpenOrders(): Result<List<UnknownOrder>, SealedApiError>
	suspend fun queryOrder(id: OrderId, pair: MarketPair): Result<UnknownOrder, SealedApiError>
	suspend fun cancelOrder(id: OrderId, pair: MarketPair): Result<UnknownOrder, SealedApiError>
}

interface WrapTraitApiLimitOrder {
	suspend fun createLimitOrder(
		pair: MarketPair,
		spend: Currency,
		spendVolume: BigDecimal,
		gainVolume: BigDecimal,
		price: BigDecimal,
	): Result<KnownOrder, SealedApiError>

	suspend fun queryOrder(order: KnownOrder): Result<KnownOrder, SealedApiError>

	//	suspend fun queryOrder(id: OrderId, pair: MarketPair): Result<UnknownOrder, SealedApiError>
	suspend fun cancelOrder(order: KnownOrder): Result<KnownOrder, SealedApiError>
//	suspend fun cancelOrder(id: OrderId, pair: MarketPair): Result<UnknownOrder, SealedApiError>
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
fun ApiWrapper.requireBalanceApi(): WrapTraitApiBalance {
	return this as? WrapTraitApiBalance ?: throw NotImplementedError("Failed requirement for balance api")
}

@Throws
fun ApiWrapper.requireKlineApi(): WrapTraitApiKline {
	return this as? WrapTraitApiKline ?: throw NotImplementedError("Failed requirement for Kline api")
}

@Throws
fun ApiWrapper.requireLimitOrderApi(): WrapTraitApiLimitOrder {
	return this as? WrapTraitApiLimitOrder ?: throw NotImplementedError("Failed requirement for limit order api")
}

@Throws
fun ApiWrapper.requireKlineWS(): WrapTraitSocketKline {
	return this as? WrapTraitSocketKline ?: throw NotImplementedError("Failed requirement for Kline ws")
}

// endregion