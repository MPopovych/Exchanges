package com.makki.exchanges.implementations

class MockClient(val handler: suspend (String) -> BasicResponse) : BasicClient(0) {

	override suspend fun get(url: String): BasicResponse {
		return handler(url)
	}

}