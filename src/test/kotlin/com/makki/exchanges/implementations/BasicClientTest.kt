package com.makki.exchanges.implementations

import com.makki.exchanges.abtractions.ClientResponse
import com.makki.exchanges.asyncTest
import kotlin.test.Test

class BasicClientTest {

	private val basicClient = BasicClient.builder().timeout(1000).build()

	@Test
	fun testError() = asyncTest {
		val response = basicClient.get("http://nonexstingwebpage.com/404")
		assert(response is ClientResponse.Error)
	}

}