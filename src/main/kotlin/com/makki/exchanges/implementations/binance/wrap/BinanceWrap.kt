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
import com.makki.exchanges.implementations.binance.models.BinanceOrderFlattened
import com.makki.exchanges.logging.defaultLogger
import com.makki.exchanges.models.*
import com.makki.exchanges.tools.CachedStateSubject
import com.makki.exchanges.tools.RateLimiterWeighted
import com.makki.exchanges.tools.StateTree
import com.makki.exchanges.tools.eqIgC
import com.makki.exchanges.wrapper.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

/**
 * This wrapper defines supported features via traits
 * Also it maps the exchange specific model onto more generic models
 * This allows to abstract away from a specific exchange in most cases
 */
open class BinanceWrap(
	private val api: BinanceApi = BinanceApi(),
	private val safeGuardConfig: SafeGuardConfig = SafeGuardConfig(),
) : ApiWrapper,
	WrapTraitApiMarketInfo, WrapTraitApiBalance, WrapTraitApiLimitOrder, WrapTraitApiOrder,
	WrapTraitErrorStream, WrapTraitApiKline, WrapTraitSocketKline,
	StateObservable {

	private val errorFlow = CachedStateSubject<ErrorNotification<SealedApiError>>(BufferOverflow.DROP_OLDEST)
	private val logger = defaultLogger()
	private val limiter = RateLimiterWeighted(
		intervalMs = safeGuardConfig.intervalMs ?: TimeUnit.SECONDS.toMillis(1),
		weightLimit = safeGuardConfig.weightLimit ?: 600f
	)
	private val klineSocket = BinanceKlineSocket()

	override suspend fun balance(): Result<BalanceBook, SealedApiError> = notify {
		if (!api.hasCredentials) return@notify SealedApiError.InvalidAuth.wrapError()

		val response = limiter.tryRun(10f) {
			api.getUserData()
		}.unwrapOk() ?: return@notify SealedApiError.RateLimited.wrapError()

		return@notify response
			.mapOk { marketInfo ->
				marketInfo.balances.map {
					BalanceEntry(
						name = it.asset,
						available = it.free,
						frozen = it.locked
					)
				}
			}.mapOk { BalanceBook(it) }
			.mapError { rcError -> rcError.toSealedError() }
	}

	override suspend fun createLimitOrder(
		pair: MarketPair,
		spend: Currency,
		spendVolume: BigDecimal,
		gainVolume: BigDecimal,
		price: BigDecimal,
	): Result<KnownOrder, SealedApiError> = notify {
		val gain = pair.getOpposite(spend)
			?: return@notify SealedApiError.Unexpected("No $spend in ${pair.prettyName()}").wrapError()

		val knownRounding = pair as? MarketPairPreciseTrait
			?: return@notify SealedApiError.Unexpected("Pair ${pair.prettyName()} has no rounding data").wrapError()

		if (!safeGuardConfig.allowOrderCreation) return@notify SealedApiError.SafeguardBlock.wrapError()

		val response = limiter.tryRun(1f) {
			val symbol = readApiName(pair)

			if (spend == pair.baseCurrency()) {
				val priceStr = price.setScale(knownRounding.quotePrecision, RoundingMode.CEILING).toPlainString()
				val baseV = spendVolume.setScale(knownRounding.basePrecision, RoundingMode.FLOOR).toPlainString()
				api.createLimitOrderInBase(symbol, side = "SELL", price = priceStr, quantity = baseV)
			} else {
				val priceStr = price.setScale(knownRounding.quotePrecision, RoundingMode.FLOOR).toPlainString()
				val quoteV = spendVolume.setScale(knownRounding.quotePrecision, RoundingMode.FLOOR).toPlainString()
				api.createLimitOrderInQuote(symbol, side = "BUY", price = priceStr, quoteOrderQty = quoteV)
			}
		}.unwrapOk() ?: return@notify SealedApiError.RateLimited.wrapError()

		return@notify response
			.mapOk { BinanceOrderFlattened.from(it) }
			.mapOk { it.toKnown(pair, spend, gain) }
			.mapError { rcError -> rcError.toSealedError() }
	}

	override suspend fun queryOrder(order: KnownOrder): Result<KnownOrder, SealedApiError> = notify {
		val response = limiter.tryRun(2f) {
			val symbol = readApiName(order.pair)
			api.getOrder(symbol, order.id.id)
		}.unwrapOk() ?: return@notify SealedApiError.RateLimited.wrapError()

		return@notify response
			.mapOk { BinanceOrderFlattened.from(it) }
			.mapOk { it.toKnown(order.pair, order.spendCurrency, order.gainCurrency) }
			.mapError { rcError -> rcError.toSealedError() }
	}

	override suspend fun cancelOrder(order: KnownOrder): Result<KnownOrder, SealedApiError> = notify {
		val response = limiter.forceRun(1f) {
			val symbol = readApiName(order.pair)
			api.cancelOrder(symbol, order.id.id)
		}

		return@notify response
			.mapOk { BinanceOrderFlattened.from(it) }
			.mapOk { it.toKnown(order.pair, order.spendCurrency, order.gainCurrency) }
			.mapError { rcError -> rcError.toSealedError() }
	}

	override suspend fun allOpenOrders(): Result<List<UnknownOrder>, SealedApiError> = notify {
		val response = limiter.tryRun(10f) {
			api.getOpenOrders()
		}.unwrapOk() ?: return@notify SealedApiError.RateLimited.wrapError()

		return@notify response
			.mapOk { list -> list.map { BinanceOrderFlattened.from(it).toUnknown() } }
			.mapError { rcError -> rcError.toSealedError() }
	}

	override suspend fun queryOrder(id: OrderId, pair: MarketPair): Result<UnknownOrder, SealedApiError> = notify {
		val response = limiter.tryRun(2f) {
			val symbol = readApiName(pair)
			api.getOrder(symbol, id.id)
		}.unwrapOk() ?: return@notify SealedApiError.RateLimited.wrapError()

		return@notify response
			.mapOk { BinanceOrderFlattened.from(it).toUnknown() }
			.mapError { rcError -> rcError.toSealedError() }
	}

	override suspend fun cancelOrder(id: OrderId, pair: MarketPair): Result<UnknownOrder, SealedApiError> = notify {
		val response = limiter.forceRun(1f) {
			val symbol = readApiName(pair)
			api.cancelOrder(symbol, id.id)
		}

		return@notify response
			.mapOk { BinanceOrderFlattened.from(it).toUnknown() }
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

	@Suppress("USELESS_CAST")
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
			.mapOk { r -> r.map { k -> k as Kline } }
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

	@Suppress("USELESS_CAST")
	override suspend fun trackKline(market: String, interval: String): Flow<Frame<Kline>> {
		klineSocket.addMarket(market, interval) // checks internally for an existing one

		return klineSocket.observe()
			.filter { it.check(true) { k -> k.market.eqIgC(market) } }
			.map { f -> f.mapAsset { k -> k as Kline } }
	}

	override suspend fun notifyError(result: ErrorNotification<SealedApiError>) {
		errorFlow.tryEmit(result)
	}

	override suspend fun trackErrors(): Flow<ErrorNotification<SealedApiError>> {
		return errorFlow.asSharedFlow()
	}

	override fun stateTree(): StateTree = StateTree()
		.merge("kline_socket", klineSocket.stateTree())
		.track("error") { errorFlow.replayCache.firstOrNull() }

	override fun readApiName(marketPair: MarketPair): String {
		return BinanceUtils.getApiMarketName(marketPair)
	}

	override fun readWSName(marketPair: MarketPair): String {
		return BinanceUtils.getApiMarketName(marketPair)
	}
}