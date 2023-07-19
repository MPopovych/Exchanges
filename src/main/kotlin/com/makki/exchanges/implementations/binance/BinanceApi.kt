package com.makki.exchanges.implementations.binance

import com.makki.exchanges.abtractions.*
import com.makki.exchanges.common.Result
import com.makki.exchanges.implementations.BasicClient
import com.makki.exchanges.implementations.binance.models.BinanceKline
import com.makki.exchanges.implementations.binance.models.BinanceMarketInfo
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

class BinanceApi(private val httpClient: Client = BasicClient.builder().build()) : RestApi {

	companion object {
		const val BASE_URL = "https://api.binance.com"
	}

	suspend fun marketInfo(): Result<BinanceMarketInfo, RemoteCallError<BinanceError>> {
		return publicApiGetMethod<BinanceMarketInfo>("api/v3/exchangeInfo", "")
	}

	suspend fun klineData(
		market: String,
		interval: String,
		limit: Int = 500,
		startMs: Long,
		endMs: Long,
	): Result<List<BinanceKline>, RemoteCallError<BinanceError>> {
		val cappedLimit = max(min(limit, 1000), 10)
		val queryMap = mapOf(
			"symbol" to market,
			"interval" to interval,
			"limit" to cappedLimit,
			"startTime" to startMs,
			"endTime" to endMs
		).toQuery()
		return publicApiGetMethod<List<BinanceKline>>("api/v3/klines", queryMap)
	}

	suspend fun klineData(
		market: String,
		interval: String,
		limit: Int = 500,
	): Result<List<BinanceKline>, RemoteCallError<BinanceError>> {
		val cappedLimit = max(min(limit, 1000), 10)
		val queryMap = mapOf(
			"symbol" to market, "interval" to interval, "limit" to cappedLimit
		).toQuery()

		return publicApiGetMethod<List<BinanceKline>>("api/v3/klines", queryMap)
	}

	private suspend inline fun <reified T : Any> publicApiGetMethod(
		path: String,
		query: String,
	): Result<T, RemoteCallError<BinanceError>> {
		val response = httpClient.get("${BASE_URL}/${path}?${query}")
		return defaultParse(response)
	}

	@Serializable
	data class BinanceError(
		val code: Int = -1,
		val msg: String = "-1",
	) : RestApi.ErrorValidator {

		override fun isNotDefault(): Boolean {
			return code != -1
		}
	}

}