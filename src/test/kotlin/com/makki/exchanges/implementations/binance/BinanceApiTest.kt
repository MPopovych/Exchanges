package com.makki.exchanges.implementations.binance

import com.makki.exchanges.nontesting.TestResourceLoader
import com.makki.exchanges.abtractions.ClientResponse
import com.makki.exchanges.abtractions.JsonParser
import com.makki.exchanges.abtractions.RemoteCallError
import com.makki.exchanges.nontesting.asyncTest
import com.makki.exchanges.nontesting.asyncTestSecure
import com.makki.exchanges.nontesting.MockClient
import io.ktor.util.network.*
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class BinanceApiTest {

	@Test
	fun testBinanceMarketInfoRequest() = asyncTestSecure("HEAVY") {
		val response = BinanceApi().marketInfo()
		assert(response.isOk()) { response.unwrapError().toString() }

		val klineList = response.unwrapOk()
		println(klineList?.symbols?.size)
	}

	@Test
	fun testBinanceKlineRequest() = asyncTestSecure("HEAVY") {
		val response = BinanceApi().klineData("BTCUSDT", "15m", limit = 10)
		assert(response.isOk()) { response.toString() }

		val klineList = response.unwrapOk()
		assert(!klineList.isNullOrEmpty())
		println(klineList)
	}

	@Test
	fun testBinanceRestErrorLayer() = asyncTest {
		val error = BinanceApi.BinanceError(14, "Test error")
		val errorJson = JsonParser.default.encodeToString(error)
		val mock = MockClient { _ -> ClientResponse.Ok(200, errorJson, 0) }
		val response = BinanceApi(mock).klineData("BTCUSDT", "15m")

		assert(response.isError() && response.unwrapError() != null)
		assert(response.unwrapError() is RemoteCallError.ApiError)
	}

	@Test
	fun testBinanceConnectionErrorLayer() = asyncTest {
		val mock = MockClient { _ -> ClientResponse.ConnectionError(UnresolvedAddressException()) }
		val response = BinanceApi(mock).klineData("BTCUSDT", "15m")

		assert(response.isError() && response.unwrapError() != null)
		assert(response.unwrapError() is RemoteCallError.ConnectionError)
	}

	@Test
	fun testBinanceParseErrorLayer() = asyncTest {
		val mock = MockClient { _ -> ClientResponse.Ok(200, "{}", 0) }
		val response = BinanceApi(mock).klineData("BTCUSDT", "15m")

		assert(response.isError() && response.unwrapError() != null)
		assert(response.unwrapError() is RemoteCallError.ParseError)
	}

	@Test
	fun testBinanceHttpErrorLayer() = asyncTest {
		val mock = MockClient { _ -> ClientResponse.Ok(404, "", 0) }
		val response = BinanceApi(mock).klineData("BTCUSDT", "15m")

		assert(response.isError() && response.unwrapError() != null)
		assert(response.unwrapError() is RemoteCallError.HttpError)
	}

	@Test
	fun testBinanceOkLayer() = asyncTest {
		val resp = TestResourceLoader.loadText("/binance/kline_rest_response.json")
		val mock = MockClient { _ -> ClientResponse.Ok(200, resp, 0) }
		val response = BinanceApi(mock).klineData("BTCUSDT", "15m")

		assert(response.isOk())
		val klineList = response.unwrapOk()
		assert(!klineList.isNullOrEmpty())
		val kline = klineList!![0]
		assert(kline.start == 1499040000000)
		assert(kline.end == 1499644799999)
		assert(kline.open == 0.01634790)
		assert(kline.high == 0.80000000)
		assert(kline.low == 0.01575800)
		assert(kline.close == 0.01577100)
	}

}