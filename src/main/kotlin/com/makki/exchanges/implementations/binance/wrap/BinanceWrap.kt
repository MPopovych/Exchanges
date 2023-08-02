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
import com.makki.exchanges.models.BalanceBook
import com.makki.exchanges.models.BalanceEntry
import com.makki.exchanges.models.Kline
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
	private val protectionConfig: SafeGuardConfig = SafeGuardConfig(),
) :
	ApiWrapper, WrapTraitApiBalance, WrapTraitApiKline,
	WrapTraitErrorStream, WrapTraitApiMarketInfo, WrapTraitSocketKline, StateObservable {

	private val errorFlow = CachedStateSubject<SealedApiError>(BufferOverflow.DROP_OLDEST)
	private val logger = defaultLogger()
	private val limiter = RateLimiterWeighted(
		intervalMs = protectionConfig.intervalMs ?: TimeUnit.SECONDS.toMillis(1),
		weightLimit = protectionConfig.weightLimit ?: 600f
	)
	private val klineSocket = BinanceKlineSocket()

	override suspend fun balance(): Result<BalanceBook, SealedApiError> = notify {
		if (api.hasCredentials) return@notify SealedApiError.InvalidAuth.wrapError()

		val response = limiter.tryRun(10f) {
			api.getUserData()
		}.unwrapOk() ?: return@notify SealedApiError.RateLimited.wrapError()

		return@notify response
			.mapOk { marketInfo ->
				marketInfo.balances.map {
					BalanceEntry(
						baseName = it.asset,
						available = it.free,
						frozen = it.locked
					)
				}
			}.mapOk {
				BalanceBook(it)
			}
			.mapError { rcError -> rcError.toSealedError() }
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
	): Result<List<Kline>, SealedApiError> = notify {
		val response = limiter.tryRun(1f) {
			if (range != null) {
				api.klineData(market, interval, limit, range.first, range.last)
			} else {
				api.klineData(market, interval, limit)
			}
		}.unwrapOk() ?: return@notify SealedApiError.RateLimited.wrapError()

		return@notify response
			.mapOk { it.map { it as Kline } }
			.mapError { rcError -> rcError.toSealedError() }
	}

	protected open fun RemoteCallError<BinanceApi.BinanceError>.toSealedError(): SealedApiError {
		return this.toSealedApiErrorExt().also {
			if (it is SealedApiError.Unexpected) {
				logger.printError("Unhandled error $it from $this")
			}
		}
	}

	override fun start() {
		klineSocket.start()
	}

	override suspend fun trackKline(market: String, interval: String): Flow<Frame<Kline>> {
		klineSocket.addMarket(market, interval) // checks internally for an existing one

		return klineSocket.observe()
			.filter { it.check(true) { k -> k.market.eqIgC(market) } }
			.map { f -> f.mapAsset { k -> k as Kline } }
	}

	override suspend fun notifyError(result: SealedApiError) {
		errorFlow.tryEmit(result)
	}

	override suspend fun trackErrors(): Flow<SealedApiError> {
		return errorFlow.asSharedFlow()
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