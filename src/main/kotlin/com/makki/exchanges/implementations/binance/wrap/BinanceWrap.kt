package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.abtractions.RestResult
import com.makki.exchanges.implementations.binance.BinanceApi
import com.makki.exchanges.implementations.binance.BinanceKlineSocket
import com.makki.exchanges.implementations.binance.BinanceUtils
import com.makki.exchanges.logging.defaultLogger
import com.makki.exchanges.models.KlineEntry
import com.makki.exchanges.models.MarketPair
import com.makki.exchanges.tools.CachedStateSubject
import com.makki.exchanges.tools.eqIgC
import com.makki.exchanges.wrapper.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

/**
 * This wrapper defines supported features via traits
 * Also it maps the exchange specific model onto more generic models
 * This allows to abstract away from a specific exchange in most cases
 */
open class BinanceWrap(private val api: BinanceApi = BinanceApi()) : ApiWrapper, WrapTraitApiKline,
	WrapTraitErrorStream, WrapTraitApiMarketInfo, WrapTraitSocketKline {

	private val errorFlow = CachedStateSubject<RestResult<*, SealedApiError>>(BufferOverflow.DROP_OLDEST)
	private val logger = defaultLogger()

	private val binanceSocket = BinanceKlineSocket()

	override suspend fun trackKline(market: String, interval: String): Flow<KlineEntry> {
		binanceSocket.addMarket(market, interval) // checks internally for an existing one

		return binanceSocket.observe()
			.filter { it.market.eqIgC(market) }
			.map { k -> binanceKlineSocketToGeneric(k) }
	}

	override suspend fun trackErrors(): Flow<RestResult<*, SealedApiError>> {
		return errorFlow.asSharedFlow()
	}

	override suspend fun marketInfo(): RestResult<List<MarketPair>, SealedApiError> {
		val response = api.marketInfo()

		return response.map { marketInfo ->
			marketInfo.symbols
				.filter { it.status == BinanceUtils.CONST_MARKET_TRADING }
				.map {
					binancePairToGeneric(it)
				}
		}.mapRestError {
			it.toSealedApiError()
		}.tryMapHttpToRestError {
			it.toSealedError()
		}.also {
			acceptResult(it)
		}
	}

	override suspend fun klineData(
		market: String,
		interval: String,
		limit: Int,
		range: LongRange?,
	): RestResult<List<KlineEntry>, SealedApiError> {
		val response = if (range != null) {
			api.klineData(market, interval, limit, range.first, range.last)
		} else {
			api.klineData(market, interval, limit)
		}
		return response.map { binanceKlines ->
			binanceKlines.map { k -> binanceKlineToGeneric(k) }
		}.mapRestError {
			it.toSealedApiError()
		}.tryMapHttpToRestError {
			it.toSealedError()
		}.also {
			acceptResult(it)
		}
	}

	/**
	 * Mapping from binance error to enum class
	 */
	protected open fun BinanceApi.BinanceError.toSealedApiError(): SealedApiError {
		return toSealedApiErrorExt().also {
			if (it is SealedApiError.Unexpected) {
				logger.printError("Unhandled error $it from $this")
			}
		}
	}

	protected open fun RestResult.HttpError<*, *>.toSealedError(): SealedApiError? {
		return toSealedErrorExt().also {
			if (it is SealedApiError.Unexpected) {
				logger.printError("Unhandled error $it from $this")
			}
		}
	}

	private fun acceptResult(result: RestResult<*, SealedApiError>) {
		errorFlow.tryEmit(result)
	}

	override fun readApiName(marketPair: MarketPair): String {
		return BinanceUtils.getApiMarketName(marketPair)
	}

}