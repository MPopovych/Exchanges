package com.makki.exchanges.implementations.binance

import com.makki.exchanges.implementations.binance.models.BinanceKlineInterval
import com.makki.exchanges.nontesting.TestLogger
import com.makki.exchanges.nontesting.asyncTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import kotlin.test.Test

class BinanceSocketTest {

	@Test
	fun testBinanceSocket() = asyncTest {
		val validCount = 5
		val socket = BinanceKlineSocket()

		socket.addMarket("btcusdt", BinanceKlineInterval.Minutes15)
		socket.start()
		delay(500)
		socket.addMarket("ethusdt", BinanceKlineInterval.Minutes15)

		val list = withTimeout(10000) {
			return@withTimeout socket.observe()
				.mapNotNull { it.unwrapAsset() }
				.onEach {
					TestLogger.logger.printDebug(it)
				}.take(validCount).toList()
		}
		assert(list.isNotEmpty() && list.size >= validCount)
		socket.stop()
	}

}