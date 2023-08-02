package com.makki.exchanges.nontesting

import com.makki.exchanges.abtractions.Client
import com.makki.exchanges.abtractions.ClientResponse

class MockClient(val handler: suspend (Client.Method, String) -> ClientResponse) : Client {
	override suspend fun get(url: String, headers: Map<String, String>): ClientResponse {
		return handler(Client.Method.GET, url)
	}

	override suspend fun post(url: String, headers: Map<String, String>, body: Client.RequestBody): ClientResponse {
		return handler(Client.Method.POST, url)
	}

	override suspend fun put(url: String, headers: Map<String, String>, body: Client.RequestBody): ClientResponse {
		return handler(Client.Method.PUT, url)
	}

	override suspend fun delete(url: String, headers: Map<String, String>, body: Client.RequestBody): ClientResponse {
		return handler(Client.Method.DELETE, url)
	}
}