package com.makki.exchanges.implementations.binance

import com.makki.exchanges.SupportedExchanges
import com.makki.exchanges.nontesting.asyncTest
import kotlin.test.Test

class BinancePrivateApiTest {

	companion object {
		// these are for a testnet spot, be nice and don't use them :)
		private const val GIT_TESTNET_API_KEY = "2hrIQazhFkpUdL8a9WvEN9ng5L8X9VAqYtrLpIxqX0o0seBQrnR5AJ2PIAX3bEsc"
		private const val GIT_TESTNET_SECRET_KEY = "hUnNsRKumXARGqGhdXTVB4iMl8Rf99jEEjnd5rU41TWE3yNUwchhQChP1BzOklcy"

		private val exchange = SupportedExchanges.BinanceTestNet(GIT_TESTNET_API_KEY, GIT_TESTNET_SECRET_KEY)
	}

	@Test
	fun testBinanceBalanceRequest() = asyncTest {
		val response = exchange.api.getUserData()

		assert(response.isOk()) { response.unwrapError().toString() }

		val balance = response.unwrapOk()
		println(balance)
	}

	@Test
	fun testBinanceOpenOrdersRequest() = asyncTest {
		val response = exchange.api.getOpenOrders()

		assert(response.isOk()) { response.unwrapError().toString() }

		val balance = response.unwrapOk()
		println(balance)
	}

	@Test
	fun testBinanceLimitOrderRequest() = asyncTest {
		val failingResponse = exchange.api.createLimitOrder("BTCUSDT", "SELL", "-0.001", "129000.0")

		assert(failingResponse.isError()) { failingResponse.unwrapOk().toString() }
		val error = failingResponse.unwrapError()
		println(error)
	}

	@Test
	fun testBinanceGetOrderRequest() = asyncTest {
		val failingResponse = exchange.api.getOrder("BTCUSDT", "13133")

		assert(failingResponse.isError()) { failingResponse.unwrapOk().toString() }
		val error = failingResponse.unwrapError()
		println(error)
	}

	@Test
	fun testBinanceCancelOrderRequest() = asyncTest {
		val failingResponse = exchange.api.cancelOrder("BTCUSDT", "1233")

		assert(failingResponse.isError()) { failingResponse.unwrapOk().toString() }
		val error = failingResponse.unwrapError()
		println(error)
	}


}