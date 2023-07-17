package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.abtractions.JsonParser
import com.makki.exchanges.asyncTest
import com.makki.exchanges.implementations.BasicResponse
import com.makki.exchanges.implementations.MockClient
import com.makki.exchanges.implementations.binance.BinanceApi
import com.makki.exchanges.implementations.binance.models.BinanceKline
import com.makki.exchanges.wrapper.SealedApiError
import kotlinx.serialization.encodeToString
import kotlin.test.Test

class BinanceWrapTest {

	@Test
	fun testBinanceRealRequest() = asyncTest {
		val response = BinanceWrap().klineData("BTCUSDT", "15m", limit = 10)
		assert(response.isOk()) { response.toString() }

		val klineList = response.unwrap<List<BinanceKline>>()
		assert(!klineList.isNullOrEmpty())
		println(klineList)
	}

	@Test
	fun testBinanceRestErrorLayer() = asyncTest {
		val errorObject = BinanceApi.BinanceError(-1109, "Test ban")
		val errorJson = JsonParser.default.encodeToString(errorObject)
		val mockedClient = MockClient { _ -> BasicResponse.Ok(200, errorJson, 0) }
		val mockedApi = BinanceApi(mockedClient)
		val response = BinanceWrap(mockedApi).klineData("BTCUSDT", "15m")
		assert(response.isRestError())
		val error = response.unwrapRestError<SealedApiError>()
		assert(error is SealedApiError.Banned)
	}

}