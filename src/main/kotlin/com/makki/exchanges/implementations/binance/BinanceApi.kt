package com.makki.exchanges.implementations.binance

import com.makki.exchanges.abtractions.Client
import com.makki.exchanges.abtractions.RemoteCallError
import com.makki.exchanges.abtractions.RestApi
import com.makki.exchanges.abtractions.defaultParse
import com.makki.exchanges.common.Result
import com.makki.exchanges.implementations.BasicClient
import com.makki.exchanges.implementations.binance.models.BinanceKline
import com.makki.exchanges.implementations.binance.models.BinanceMarketInfo
import com.makki.exchanges.implementations.binance.models.BinanceOrder_RESULT
import com.makki.exchanges.implementations.binance.models.BinanceUserData
import com.makki.exchanges.logging.defaultLogger
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

open class BinanceApi(
	private val httpClient: Client = BasicClient.builder().build(),
	private val pubKey: String = "",
	private val secretKey: String = "",
	baseUrl: String? = null,
) : RestApi {

	companion object {
		const val TEST_SPOT_API_URL = "https://testnet.binance.vision"
		const val SPOT_API_URL = "https://api.binance.com"
	}

	private val baseUrl = baseUrl ?: SPOT_API_URL
	private val logger = defaultLogger()

	val hasCredentials = pubKey.isNotBlank() && secretKey.isNotBlank()

	suspend fun marketInfo(): Result<BinanceMarketInfo, RemoteCallError<BinanceError>> {
		return publicApiGetMethod<BinanceMarketInfo>("api/v3/exchangeInfo", emptyMap())
	}

	suspend fun klineData(
		symbol: String,
		interval: String,
		limit: Int = 500,
		startMs: Long,
		endMs: Long,
	): Result<List<BinanceKline>, RemoteCallError<BinanceError>> {
		val cappedLimit = max(min(limit, 1000), 10)
		val queryMap = linkedMapOf(
			"symbol" to symbol,
			"interval" to interval,
			"limit" to cappedLimit,
			"startTime" to startMs,
			"endTime" to endMs
		)
		return publicApiGetMethod<List<BinanceKline>>("api/v3/klines", queryMap)
	}

	suspend fun klineData(
		symbol: String,
		interval: String,
		limit: Int = 500,
	): Result<List<BinanceKline>, RemoteCallError<BinanceError>> {
		val cappedLimit = max(min(limit, 1000), 10)
		val queryMap = linkedMapOf(
			"symbol" to symbol, "interval" to interval, "limit" to cappedLimit
		)

		return publicApiGetMethod<List<BinanceKline>>("api/v3/klines", queryMap)
	}

	private suspend inline fun <reified T : Any> publicApiGetMethod(
		path: String,
		queryMap: Map<String, Any>,
	): Result<T, RemoteCallError<BinanceError>> {
		val queryPart = queryMap.let { if (it.isEmpty()) "" else "?${it.toQueryEncodedUTF8()}" }
		val response = httpClient.get("${baseUrl}/${path}${queryPart}")
		return defaultParse(response)
	}

	suspend fun getUserData(): Result<BinanceUserData, RemoteCallError<BinanceError>> {
		return privateApiGetMethod("api/v3/account", emptyMap(), Client.Method.GET)
	}

	suspend fun getOpenOrders(): Result<List<BinanceOrder_RESULT>, RemoteCallError<BinanceError>> {
		return privateApiGetMethod("api/v3/openOrders", emptyMap(), Client.Method.GET)
	}

	suspend fun getOrder(
		symbol: String,
		orderId: String,
	): Result<BinanceOrder_RESULT, RemoteCallError<BinanceError>> {
		val queryMap = linkedMapOf(
			"symbol" to symbol, // binance require uppercase
			"orderId" to orderId
		)
		return privateApiGetMethod("api/v3/order", queryMap, Client.Method.GET)
	}

	suspend fun cancelOrder(
		symbol: String,
		orderId: String,
	): Result<BinanceOrder_RESULT, RemoteCallError<BinanceError>> {
		val queryMap = linkedMapOf(
			"symbol" to symbol, // binance require uppercase
			"orderId" to orderId
		)
		return privateApiGetMethod("api/v3/order", queryMap, Client.Method.DELETE)
	}

	suspend fun createLimitOrderInBase(
		symbol: String,
		side: String,
		quantityBase: String,
		price: String,
		timeInForce: String = "GTC",
	): Result<BinanceOrder_RESULT, RemoteCallError<BinanceError>> {
		val queryMap = linkedMapOf(
			"symbol" to symbol,
			"side" to side,
			"price" to price,
			"quantity" to quantityBase,
			"type" to "LIMIT",
			"newOrderRespType" to "FULL",
			"timeInForce" to timeInForce
		)
		return privateApiGetMethod("api/v3/order", queryMap, Client.Method.POST)
	}

	suspend fun createLimitOrderInQuote(
		symbol: String,
		side: String,
		quantityBase: String,
		price: String,
		timeInForce: String = "GTC",
	): Result<BinanceOrder_RESULT, RemoteCallError<BinanceError>> {
		val queryMap = linkedMapOf(
			"symbol" to symbol,
			"side" to side,
			"price" to price,
			"quantity" to quantityBase,
			"type" to "LIMIT",
			"newOrderRespType" to "FULL",
			"timeInForce" to timeInForce
		)
		return privateApiGetMethod("api/v3/order", queryMap, Client.Method.POST)
	}

	private suspend inline fun <reified T : Any> privateApiGetMethod(
		path: String,
		queryMap: Map<String, Any>,
		method: Client.Method,
	): Result<T, RemoteCallError<BinanceError>> {
		if (!hasCredentials) {
			logger.printWarning("Calling: ${baseUrl}/${path} with no credentials")
		}

		val internalArgs = LinkedHashMap(queryMap) // timestamp need to be last
		internalArgs["recvWindow"] = 59999
		internalArgs["timestamp"] = System.currentTimeMillis()
		internalArgs["signature"] = toHex(toHmacSha256(internalArgs.toQueryEncodedUTF8(), secretKey))

		val internalHeader = mapOf(
			"X-MBX-APIKEY" to pubKey
		)

		val queryPart = internalArgs.toQueryEncodedUTF8()
		val url = "${baseUrl}/${path}?${queryPart}"
		val response = when (method) {
			Client.Method.GET -> httpClient.get(url = url, headers = internalHeader)
			Client.Method.POST -> httpClient.post(url = url, headers = internalHeader)
			Client.Method.PUT -> httpClient.put(url = url, headers = internalHeader)
			Client.Method.DELETE -> httpClient.delete(url = url, headers = internalHeader)
		}
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