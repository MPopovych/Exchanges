package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.abtractions.RestResult
import com.makki.exchanges.implementations.binance.BinanceApi
import com.makki.exchanges.logging.printLogRed
import com.makki.exchanges.tools.CachedSubject
import com.makki.exchanges.wrapper.ApiWrapper
import com.makki.exchanges.wrapper.SealedApiError
import com.makki.exchanges.wrapper.WrapTraitApiKline
import com.makki.exchanges.wrapper.WrapTraitErrorStream
import com.makki.exchanges.wrapper.models.KlineEntry
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * This wrapper defines supported features via traits
 * Also it maps the exchange specific model onto more generic models
 * This allows to abstract away from a specific exchange in most cases
 */
open class BinanceWrap(private val api: BinanceApi = BinanceApi()) : ApiWrapper, WrapTraitApiKline, WrapTraitErrorStream {

	private val errorFlow = CachedSubject<RestResult<*, SealedApiError>>(BufferOverflow.DROP_OLDEST)

	override suspend fun trackErrors(): Flow<RestResult<*, SealedApiError>> {
		return errorFlow.asSharedFlow()
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
			it.toSealedApiError(market = market)
		}.mapHttpErrorToRestError {
			it.toSealedError()
		}.also {
			acceptResult(it)
		}
	}

	/**
	 * Mapping from binance error to enum class
	 */
	protected open fun BinanceApi.BinanceError.toSealedApiError(
		market: String? = null,
		orderId: String? = null,
	): SealedApiError {
		return toSealedApiErrorExt(market, orderId).also {
			this@BinanceWrap.printLogRed("Unhandled error $it from $this")
		}
	}

	protected open fun RestResult.HttpError<*, *>.toSealedError(): SealedApiError? {
		return toSealedErrorExt().also {
			this@BinanceWrap.printLogRed("Unhandled error $it from $this")
		}
	}

	private fun acceptResult(result: RestResult<*, SealedApiError>) {
		errorFlow.tryEmit(result)
	}

}