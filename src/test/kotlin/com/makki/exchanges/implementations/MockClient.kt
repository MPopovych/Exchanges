package com.makki.exchanges.implementations

import com.makki.exchanges.abtractions.Client
import com.makki.exchanges.abtractions.ClientResponse

class MockClient(val handler: suspend (String) -> ClientResponse) : Client {
	override suspend fun get(url: String): ClientResponse {
		return handler(url)
	}
}