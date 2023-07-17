package com.makki.exchanges.implementations

import com.makki.exchanges.asyncTest
import kotlinx.coroutines.delay
import kotlin.test.Test

class BasicSocketTest {

	@Test
	fun testMock() = asyncTest {
		val validationMsg = "Hello, world"
		val buffer = HashSet<String>()
		val socket = SelfManagingSocket.builder("MockSocket")
			.url("wss://www.nonexistingwebpage.com:4443/stream")
			.socket(MockSocket {
				validationMsg
			})
			.onTextMsg {
				println("Received msg: $it")
				buffer.add(it)
			}
			.build()
		socket.start()

		delay(2000)
		assert(buffer.size == 1)
		assert(validationMsg in buffer)
	}

}