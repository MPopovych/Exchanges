package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.abtractions.Frame
import com.makki.exchanges.abtractions.RemoteCallError
import com.makki.exchanges.abtractions.StateObservable
import com.makki.exchanges.common.Result
import com.makki.exchanges.common.mapError
import com.makki.exchanges.common.mapOk
import com.makki.exchanges.common.wrapError
import com.makki.exchanges.implementations.binance.BinanceApi
import com.makki.exchanges.implementations.binance.BinanceKlineSocket
import com.makki.exchanges.implementations.binance.BinanceUtils
import com.makki.exchanges.logging.defaultLogger
import com.makki.exchanges.models.KlineEntry
import com.makki.exchanges.models.MarketPair
import com.makki.exchanges.tools.CachedStateSubject
import com.makki.exchanges.tools.RateLimiterWeighted
import com.makki.exchanges.tools.StateObserver
import com.makki.exchanges.tools.eqIgC
import com.makki.exchanges.wrapper.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * This wrapper defines supported features via traits
 * Also it maps the exchange specific model onto more generic models
 * This allows to abstract away from a specific exchange in most cases
 */
open class BinanceWrap(
	private val api: BinanceApi = BinanceApi(),
	private val apiConfig: ApiConfig = ApiConfig(),
) :
	ApiWrapper, WrapTraitApiKline,
	WrapTraitErrorStream, WrapTraitApiMarketInfo, WrapTraitSocketKline, StateObservable {

	private val errorFlow = CachedStateSubject<SealedApiError>(BufferOverflow.DROP_OLDEST)
	private val logger = defaultLogger()
	private val limiter = RateLimiterWeighted(
		intervalMs = apiConfig.intervalMs ?: TimeUnit.SECONDS.toMillis(1),
		weightLimit = apiConfig.weightLimit ?: 600f
	)
	private val klineSocket = BinanceKlineSocket()

	override fun start() {
		klineSocket.start()
	}

	override suspend fun trackKline(market: String, interval: String): Flow<Frame<KlineEntry>> {
		klineSocket.addMarket(market, interval) // checks internally for an existing one

		return klineSocket.observe()
			.filter { it.check(true) { k -> k.market.eqIgC(market) } }
			.map { f -> f.mapAsset { k -> binanceKlineSocketToGeneric(k) } }
	}

	override suspend fun trackErrors(): Flow<SealedApiError> {
		return errorFlow.asSharedFlow()
	}

	override suspend fun marketInfo(): Result<List<MarketPair>, SealedApiError> = notify {
		val response = limiter.tryRun(10f) {
			api.marketInfo()
		}.unwrapOk() ?: return@notify SealedApiError.RateLimited.wrapError()

		return@notify response
			.mapOk { marketInfo ->
				marketInfo.symbols
					.filter { it.status == BinanceUtils.CONST_MARKET_TRADING }
					.map { pair -> binancePairToGeneric(pair) }
			}
			.mapError { rcError -> rcError.toSealedError() }
	}

	override suspend fun klineData(
		market: String,
		interval: String,
		limit: Int,
		range: LongRange?,
	): Result<List<KlineEntry>, SealedApiError> = notify {
		val response = limiter.tryRun(1f) {
			if (range != null) {
				api.klineData(market, interval, limit, range.first, range.last)
			} else {
				api.klineData(market, interval, limit)
			}
		}.unwrapOk() ?: return@notify SealedApiError.RateLimited.wrapError()

		return@notify response
			.mapOk { binanceKlines ->
				binanceKlines.map { k -> binanceKlineToGeneric(k) }
			}
			.mapError { rcError -> rcError.toSealedError() }
	}

	protected open fun RemoteCallError<BinanceApi.BinanceError>.toSealedError(): SealedApiError {
		return this.toSealedApiErrorExt().also {
			if (it is SealedApiError.Unexpected) {
				logger.printError("Unhandled error $it from $this")
			}
		}
	}

	override suspend fun notifyError(result: SealedApiError) {
		errorFlow.tryEmit(result)
	}

	override fun state(): StateObserver = StateObserver()
		.merge("kline_socket", klineSocket.state())
		.track("error") { errorFlow.replayCache.firstOrNull() as Any }

	override fun readApiName(marketPair: MarketPair): String {
		return BinanceUtils.getApiMarketName(marketPair)
	}

	override fun readWSName(marketPair: MarketPair): String {
		return BinanceUtils.getApiMarketName(marketPair)
	}

}