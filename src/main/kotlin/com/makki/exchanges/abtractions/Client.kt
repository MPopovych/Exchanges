package com.makki.exchanges.abtractions

interface Client {
	suspend fun get(url: String): ClientResponse
}

sealed interface ClientResponse {
	data class Ok(
		val httpCode: Int,
		val text: String,
		val time: Long,
	) : ClientResponse

	class Error(val e: Exception) : ClientResponse
}