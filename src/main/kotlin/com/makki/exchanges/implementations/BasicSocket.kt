package com.makki.exchanges.implementations

import com.makki.exchanges.abtractions.SocketApi
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

open class BasicSocket(
	private val connectTimeoutMs: Long = 5000,
	private val pingIntervalMs: Long = 15000,
) : SocketApi {

	init {
		// unsafe otherwise
		require(connectTimeoutMs > 0 && connectTimeoutMs < TimeUnit.MINUTES.toMillis(1))
		require(pingIntervalMs > 2000 && pingIntervalMs < TimeUnit.MINUTES.toMillis(1))
	}

	private val socket = HttpClient(OkHttp) {
		engine {
			this.preconfigured = OkHttpClient.Builder()
				.callTimeout(Duration.ofMillis(connectTimeoutMs))
				.readTimeout(Duration.ofMillis(connectTimeoutMs))
				.writeTimeout(Duration.ofMillis(connectTimeoutMs))
				.pingInterval(Duration.ofMillis(pingIntervalMs))
				.build()
		}
		install(WebSockets) {
			this.pingInterval = pingIntervalMs
		}
	}

	override suspend fun connect(url: String, block: suspend SocketSession.() -> Unit) {
		socket.webSocket(urlString = url) {
			block(BasicSocketSession(this))
		}
	}
}

/**
 * Hides the internal implementation and allows for mocking
 */
interface SocketSession {
	suspend fun receive(): SocketFrame
	suspend fun send(text: String): Boolean
	suspend fun send(byteArray: ByteArray): Boolean
	fun cancel(e: CancellationException? = null)
	fun isActive(): Boolean
}

sealed interface SocketFrame {
	class Text(val data: String) : SocketFrame
	class Binary(val data: ByteArray) : SocketFrame
	class Close(val reason: String) : SocketFrame
	object PingPong : SocketFrame
}

class BasicSocketSession(private val ktorSession: DefaultClientWebSocketSession) : SocketSession {
	override suspend fun receive(): SocketFrame {
		val msg = try {
			ktorSession.incoming.receive()
		} catch (e: ClosedReceiveChannelException) {
			return SocketFrame.Close(e.message ?: "ClosedReceiveChannelException")
		}
		return when (msg) {
			is Frame.Binary -> SocketFrame.Binary(msg.readBytes())
			is Frame.Close -> SocketFrame.Close(msg.readReason()?.message ?: "Unknown")
			is Frame.Text -> SocketFrame.Text(msg.readText())
			else -> SocketFrame.PingPong
		}
	}

	override suspend fun send(text: String): Boolean {
		return try {
			ktorSession.send(text)
			true
		} catch (e: Exception) {
			false
		}
	}

	override suspend fun send(byteArray: ByteArray): Boolean {
		return try {
			ktorSession.send(byteArray)
			true
		} catch (e: Exception) {
			false
		}
	}

	override fun cancel(e: CancellationException?) {
		try {
			ktorSession.cancel(e)
		} catch (_: IllegalStateException) {
		}
	}

	override fun isActive(): Boolean {
		return ktorSession.isActive
	}
}