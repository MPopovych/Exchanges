package com.makki.exchanges.abtractions

import com.makki.exchanges.implementations.SocketSession

interface SocketApi {
	suspend fun connect(url: String, block: suspend SocketSession.() -> Unit)
}