package com.makki.exchanges.implementations

import com.makki.exchanges.nontesting.MockSocket
import com.makki.exchanges.nontesting.asyncTest
import com.makki.exchanges.tools.FreshOnlySubject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.timeout
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

class BasicSocketTest {

	@OptIn(FlowPreview::class)
	@Test
	fun testMock() = asyncTest {
		val validationMsg = "Hello, world"

		// this subject is later on subscribed with a timeout
		val flow = FreshOnlySubject<String>()

		val socket = SelfManagingSocket.builder("MockSocket")
			.url("wss://www.nonexistingwebpage.com:4443/stream")
			.socket(MockSocket(intervalMs = 100) {
				validationMsg
			})
			.onTextMsg {
				flow.emit(it)
			}
			.build()
		socket.start()

		val any = flow.timeout(1200.milliseconds).first()
		assert(validationMsg == any)
	}

}