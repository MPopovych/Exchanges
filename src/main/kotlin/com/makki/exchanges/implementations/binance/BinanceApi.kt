package com.makki.exchanges.implementations.binance

import com.makki.exchanges.abtractions.Client
import com.makki.exchanges.abtractions.RestApi
import com.makki.exchanges.abtractions.RestResult
import com.makki.exchanges.abtractions.defaultParse
import com.makki.exchanges.implementations.BasicClient
import com.makki.exchanges.implementations.binance.models.BinanceKline
import com.makki.exchanges.implementations.binance.models.BinanceMarketInfo
import kotlinx.serialization.Serializable

class BinanceApi(private val httpClient: Client = BasicClient.builder().build()) : RestApi {

	companion object {
		const val BASE_URL = "https://api.binance.com"
	}

	suspend fun marketInfo(): RestResult<BinanceMarketInfo, BinanceError> {
		return publicApiGetMethod<BinanceMarketInfo>("api/v3/exchangeInfo", "")
	}

	suspend fun klineData(
		market: String,
		interval: String,
		limit: Int = 500,
		startTime: Long,
		endTime: Long,
	): RestResult<List<BinanceKline>, BinanceError> {
		val queryMap = mapOf(
			"symbol" to market, "interval" to interval, "limit" to limit, "startTime" to startTime, "endTime" to endTime
		).toQuery()
		return publicApiGetMethod<List<BinanceKline>>("api/v3/klines", queryMap)
	}

	suspend fun klineData(
		market: String,
		interval: String,
		limit: Int = 500,
	): RestResult<List<BinanceKline>, BinanceError> {
		val queryMap = mapOf(
			"symbol" to market, "interval" to interval, "limit" to limit
		).toQuery()

		return publicApiGetMethod<List<BinanceKline>>("api/v3/klines", queryMap)
	}

	private suspend inline fun <reified T : Any> publicApiGetMethod(
		path: String,
		query: String,
	): RestResult<T, BinanceError> {
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