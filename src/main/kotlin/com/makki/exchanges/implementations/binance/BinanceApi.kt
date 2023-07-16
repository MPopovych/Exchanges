package com.makki.exchanges.implementations.binance

import com.makki.exchanges.abtractions.RestApi
import com.makki.exchanges.abtractions.RestResult
import com.makki.exchanges.abtractions.defaultParse
import com.makki.exchanges.implementations.BasicClient
import kotlinx.serialization.Serializable

class BinanceApi(private val httpClient: BasicClient) : RestApi {

	constructor() : this(BasicClient.builder().build())

	companion object {
		const val BASE_URL = "https://api.binance.com"
	}

	suspend fun klineData(
		market: String,
		interval: String,
		limit: Int = 500,
		startTime: Long,
		endTime: Long,
	): RestResult<List<BinanceKline>, BinanceError> {
		val queryMap = mapOf(
			"symbol" to market,
			"interval" to interval,
			"limit" to limit,
			"startTime" to startTime,
			"endTime" to endTime
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

	private suspend inline fun <reified T> publicApiGetMethod(
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