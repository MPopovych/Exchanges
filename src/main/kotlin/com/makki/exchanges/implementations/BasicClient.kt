package com.makki.exchanges.implementations

import com.makki.exchanges.abtractions.ClientResponse
import com.makki.exchanges.abtractions.Client
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.util.concurrent.TimeUnit

open class BasicClient internal constructor(
	timeout: Long,
) : Client {

	companion object {
		fun builder() = ClientBuilder()
	}

	private val httpClient = HttpClient(CIO) {
		engine {
			requestTimeout = timeout
		}
	}

	override suspend fun get(url: String): ClientResponse {
		val response: HttpResponse = try {
			httpClient.get(urlString = url)
		} catch (e: Exception) {
			return ClientResponse.ConnectionError(e)
		}
		val text = response.bodyAsText()
		val httpCode = response.status.value
		val time = response.responseTime.timestamp - response.requestTime.timestamp
		return ClientResponse.Ok(httpCode, text, time)
	}
}

class ClientBuilder {
	private var timeout: Long = TimeUnit.MILLISECONDS.toSeconds(8)

	fun timeout(ms: Long): ClientBuilder {
		timeout = ms
		return this
	}

	fun build(): BasicClient {
		return BasicClient(timeout = timeout)
	}
}