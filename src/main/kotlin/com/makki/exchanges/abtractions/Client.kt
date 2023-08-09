package com.makki.exchanges.abtractions

interface Client {
	enum class Method {
		GET,
		PUT,
		POST,
		DELETE
	}

	sealed interface RequestBody {
		object None : RequestBody
		class Json(val json: String) : RequestBody
		class Form(val map: Map<String, Any>) : RequestBody
	}

	suspend fun get(url: String, headers: Map<String, String> = emptyMap()): ClientResponse
	suspend fun post(
		url: String,
		headers: Map<String, String> = emptyMap(),
		body: RequestBody = RequestBody.None,
	): ClientResponse

	suspend fun put(
		url: String,
		headers: Map<String, String> = emptyMap(),
		body: RequestBody = RequestBody.None,
	): ClientResponse

	suspend fun delete(
		url: String,
		headers: Map<String, String> = emptyMap(),
		body: RequestBody = RequestBody.None,
	): ClientResponse
}

sealed interface ClientResponse {
	data class Ok(
		val httpCode: Int,
		val text: String,
		val time: Long,
	) : ClientResponse

	class ConnectionError(val e: Exception) : ClientResponse
}