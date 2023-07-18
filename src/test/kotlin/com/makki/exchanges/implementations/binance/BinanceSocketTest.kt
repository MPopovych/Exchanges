package com.makki.exchanges.implementations.binance

import com.makki.exchanges.asyncTest
import com.makki.exchanges.implementations.SelfManagingSocket
import com.makki.exchanges.implementations.binance.models.BinanceKlineInterval
import com.makki.exchanges.logging.printLog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test

class BinanceSocketTest {

	private val subMsg = """
{
  "method": "SUBSCRIBE",
  "params": [
    "btcusdt@kline_15m"
  ],
  "id": 1
}
""".trimIndent()

	@Test
	fun testBinanceSocket() = asyncTest {
		val socket = BinanceKlineSocket(BinanceKlineInterval.Minutes15)
		socket.addMarket("btcusdt")
		socket.start()

		delay(300)
		socket.addMarket("ethusdt")

		var job: Job? = null
		with(CoroutineScope(coroutineContext)) {
			job = launch {
				socket.observe().collect {
					this@BinanceSocketTest.printLog(it)
				}
			}
		}

		delay(5000)
		socket.stop()
		delay(1000)
		job?.cancel()
	}

	@Test
	fun testSelfManagingRunAndExternalClose() = asyncTest {

		val buffer = ArrayList<String>()

		val socket = SelfManagingSocket.builder("TestSocketBinance")
			.url("wss://stream.binance.com:9443/ws/stream")
			.onConnectionOpen { this.send(subMsg) }
			.onTextMsg {
				println("received msg:\n${it}")
				buffer.add(it)
			}
			.build()

		socket.start()

		delay(5000)
		socket.close()
		delay(2000)

		assert(buffer.isNotEmpty())
	}

	@Test
	fun testSelfManagingInnerClose() = asyncTest {
		val buffer = ArrayList<String>()

		val socket = SelfManagingSocket.builder("TestSocketBinance")
			.url("wss://stream.binance.com:9443/ws/stream")
			.onConnectionOpen { this.send(subMsg) }
			.onTextMsg {
				println("received msg:\n${it}")
				buffer.add(it)
				this.close()
			}
			.build()

		socket.start()

		delay(6000)

		assert(buffer.isNotEmpty() && buffer.size == 1)
	}
}