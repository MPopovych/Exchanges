package com.makki.exchanges.implementations

import com.makki.exchanges.abtractions.SocketApi
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

open class BasicSocket : SocketApi {
	private val socket = HttpClient(CIO) {
		engine {
			requestTimeout = 5000
		}
		install(WebSockets)
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
	suspend fun send(text: String)
	suspend fun send(byteArray: ByteArray)
	fun cancel(e: CancellationException? = null)
	fun isActive(): Boolean
}

sealed interface SocketFrame {
	class Text(val data: String) : SocketFrame
	class Binary(val data: ByteArray) : SocketFrame
	class Close(val reason: String) : SocketFrame
	object Unknown : SocketFrame
}

class BasicSocketSession(private val ktorSession: DefaultClientWebSocketSession) : SocketSession {
	override suspend fun receive(): SocketFrame {
		return when (val msg = ktorSession.incoming.receive()) {
			is Frame.Binary -> SocketFrame.Binary(msg.readBytes())
			is Frame.Close -> SocketFrame.Close(msg.readReason()?.message ?: "Unknown")
			is Frame.Text -> SocketFrame.Text(msg.readText())
			else -> SocketFrame.Unknown
		}
	}

	override suspend fun send(text: String) {
		ktorSession.send(text)
	}

	override suspend fun send(byteArray: ByteArray) {
		ktorSession.send(byteArray)
	}

	override fun cancel(e: CancellationException?) {
		ktorSession.cancel(e)
	}

	override fun isActive(): Boolean {
		return ktorSession.isActive
	}


}