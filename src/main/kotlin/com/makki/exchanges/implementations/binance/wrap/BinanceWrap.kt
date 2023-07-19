package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.abtractions.RemoteCallError
import com.makki.exchanges.common.Result
import com.makki.exchanges.common.mapError
import com.makki.exchanges.common.mapOk
import com.makki.exchanges.common.onError
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * This wrapper defines supported features via traits
 * Also it maps the exchange specific model onto more generic models
 * This allows to abstract away from a specific exchange in most cases
 */
open class BinanceWrap(private val api: BinanceApi = BinanceApi()) : ApiWrapper, WrapTraitApiKline,
	WrapTraitErrorStream, WrapTraitApiMarketInfo, WrapTraitSocketKline {

	private val errorFlow = CachedStateSubject<SealedApiError>(BufferOverflow.DROP_OLDEST)
	private val logger = defaultLogger()

	private val binanceSocket = BinanceKlineSocket()

	override suspend fun trackKline(market: String, interval: String): Flow<KlineEntry> {
		binanceSocket.addMarket(market, interval) // checks internally for an existing one

		return binanceSocket.observe()
			.filter { it.market.eqIgC(market) }
			.map { k -> binanceKlineSocketToGeneric(k) }
	}

	override suspend fun trackErrors(): Flow<SealedApiError> {
		return errorFlow.asSharedFlow()
	}

	override suspend fun marketInfo(): Result<List<MarketPair>, SealedApiError> {
		val response = api.marketInfo()

		return response
			.mapOk { marketInfo ->
				marketInfo.symbols
					.filter { it.status == BinanceUtils.CONST_MARKET_TRADING }
					.map { pair ->
						binancePairToGeneric(pair)
					}
			}
			.mapError { rcError -> rcError.toSealedError() }
			.onError { sealed -> notifyError(sealed) }
	}

	override suspend fun klineData(
		market: String,
		interval: String,
		limit: Int,
		range: LongRange?,
	): Result<List<KlineEntry>, SealedApiError> {
		val response = if (range != null) {
			api.klineData(market, interval, limit, range.first, range.last)
		} else {
			api.klineData(market, interval, limit)
		}
		return response
			.mapOk { binanceKlines ->
				binanceKlines.map { k -> binanceKlineToGeneric(k) }
			}
			.mapError { rcError -> rcError.toSealedError() }
			.onError { sealed -> notifyError(sealed) }
	}

	protected open fun RemoteCallError<BinanceApi.BinanceError>.toSealedError(): SealedApiError {
		return this.toSealedApiErrorExt().also {
			if (it is SealedApiError.Unexpected) {
				logger.printError("Unhandled error $it from $this")
			}
		}
	}

	private fun notifyError(result: SealedApiError) {
		errorFlow.tryEmit(result)
	}

	override fun readApiName(marketPair: MarketPair): String {
		return BinanceUtils.getApiMarketName(marketPair)
	}

}