package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.abtractions.ClientResponse
import com.makki.exchanges.implementations.binance.BinanceApi
import com.makki.exchanges.nontesting.MockClient
import com.makki.exchanges.nontesting.TestResourceLoader
import com.makki.exchanges.nontesting.asyncTest
import com.makki.exchanges.nontesting.asyncTestSecure
import com.makki.exchanges.wrapper.SealedApiError
import kotlin.test.Test

class BinanceWrapTest {

	@Test
	fun testBinanceMarketInfoRequest() = asyncTestSecure("HEAVY") {
		val response = BinanceWrap().marketInfo()
		assert(response.isOk()) { response }

		val marketInfo = response.unwrapOk()
		assert(!marketInfo.isNullOrEmpty())
		println(marketInfo?.take(5))
	}

	@Test
	fun testBinanceKlineRequest() = asyncTestSecure("HEAVY") {
		val response = BinanceWrap().klineData("BTCUSDT", "15m", limit = 10)
		assert(response.isOk()) { response.toString() }

		val klineList = response.unwrapOk()
		assert(!klineList.isNullOrEmpty())
		println(klineList?.take(5))
	}

	@Test
	fun testBinanceMockMarketInfo() = asyncTest {
		val marketInfoJson = TestResourceLoader.loadText("/binance/market_info.json")
		val mockedClient = MockClient { _, _ -> ClientResponse.Ok(200, marketInfoJson, 0) }
		val mockedApi = BinanceApi(mockedClient)
		val response = BinanceWrap(mockedApi).marketInfo()
		assert(response.isOk())
		val model = response.unwrapOk()
		assert(!model.isNullOrEmpty())
		println(model?.take(3))
	}

	@Test
	fun testBinanceRestErrorLayer() = asyncTest {
		val errorJson = TestResourceLoader.loadText("/binance/ban_response.json")
		val mockedClient = MockClient { _, _ -> ClientResponse.Ok(200, errorJson, 0) }
		val mockedApi = BinanceApi(mockedClient)
		val response = BinanceWrap(mockedApi).klineData("BTCUSDT", "15m")
		assert(response.isError())
		val error = response.unwrapError()
		assert(error is SealedApiError.Banned)
	}

}