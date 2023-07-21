package com.makki.exchanges.implementations

import com.makki.exchanges.abtractions.Client
import com.makki.exchanges.abtractions.ClientResponse
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import okhttp3.OkHttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

open class BasicClient internal constructor(
	connectTimeoutMs: Long,
) : Client {

	companion object {
		fun builder() = ClientBuilder()
	}

	private val httpClient = HttpClient(OkHttp) {
		engine {
			this.preconfigured = OkHttpClient.Builder()
				.callTimeout(Duration.ofMillis(connectTimeoutMs))
				.readTimeout(Duration.ofMillis(connectTimeoutMs))
				.writeTimeout(Duration.ofMillis(connectTimeoutMs))
				.build()
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
		return BasicClient(connectTimeoutMs = timeout)
	}
}