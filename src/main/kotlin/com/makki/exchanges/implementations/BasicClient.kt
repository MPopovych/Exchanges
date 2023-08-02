package com.makki.exchanges.implementations

import com.makki.exchanges.abtractions.Client
import com.makki.exchanges.abtractions.ClientResponse
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
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

	override suspend fun get(url: String, headers: Map<String, String>): ClientResponse {
		return genericWithBody(HttpMethod.Get, url, headers, Client.RequestBody.None)
	}

	override suspend fun put(url: String, headers: Map<String, String>, body: Client.RequestBody): ClientResponse {
		return genericWithBody(HttpMethod.Put, url, headers, body)
	}

	override suspend fun post(url: String, headers: Map<String, String>, body: Client.RequestBody): ClientResponse {
		return genericWithBody(HttpMethod.Post, url, headers, body)
	}

	override suspend fun delete(url: String, headers: Map<String, String>, body: Client.RequestBody): ClientResponse {
		return genericWithBody(HttpMethod.Delete, url, headers, body)
	}

	private suspend fun genericWithBody(
		method: HttpMethod,
		url: String,
		headers: Map<String, String>,
		body: Client.RequestBody,
	): ClientResponse {
		val response: HttpResponse = try {
			httpClient.request(urlString = url) {
				this.method = method
				headers {
					for ((key, value) in headers) {
						append(key, value)
					}
				}
				when (body) {
					is Client.RequestBody.Form -> {
						formData {
							for ((k, v) in body.map) {
								append(k, v.toString())
							}
						}
					}

					is Client.RequestBody.Json -> {
						contentType(ContentType.Application.Json)
						setBody(body.json)
					}

					Client.RequestBody.None -> {
						// nothing
					}
				}
			}
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