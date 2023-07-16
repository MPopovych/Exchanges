package com.makki.exchanges.implementations.binance

import com.makki.exchanges.asyncTest
import com.makki.exchanges.implementations.SelfManagingSocket
import kotlinx.coroutines.delay
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
	fun testSelfManagingRunAndExternalClose() = asyncTest {

		val buffer = ArrayList<String>()

		val socket = SelfManagingSocket.builder<Unit>("TestSocketBinance")
			.url("wss://stream.binance.com:9443/ws/stream")
			.onConnectionOpen { this.send(subMsg) }
			.onTextMsg {
				println("received msg:\n${it}")
				buffer.add(it)
			}
			.build()

		socket.start()

		delay(10000)
		socket.close()
		delay(8000)

		assert(buffer.isNotEmpty())
	}

	@Test
	fun testSelfManagingInnerClose() = asyncTest {
		val buffer = ArrayList<String>()

		val socket = SelfManagingSocket.builder<Unit>("TestSocketBinance")
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