package com.makki.exchanges.implementations.binance

import com.makki.exchanges.TestLogger
import com.makki.exchanges.asyncTest
import com.makki.exchanges.implementations.binance.models.BinanceKlineInterval
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import kotlin.test.Test

class BinanceSocketTest {

	@Test
	fun testBinanceSocket() = asyncTest {
		val validCount = 5
		val socket = BinanceKlineSocket(BinanceKlineInterval.Minutes15)

		socket.addMarket("btcusdt")
		socket.start()
		delay(500)
		socket.addMarket("ethusdt")

		val list = withTimeout(10000) {
			return@withTimeout socket.observe().onEach {
				TestLogger.logger.printDebug(it)
			}.take(validCount).toList()
		}
		assert(list.isNotEmpty() && list.size >= validCount)
		socket.stop()
	}

}