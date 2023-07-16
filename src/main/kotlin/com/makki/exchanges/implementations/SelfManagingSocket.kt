package com.makki.exchanges.implementations

import com.makki.exchanges.tools.CachedSubject
import com.makki.exchanges.tools.RetryTimer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class SocketBuilder(private val name: String) {
	private var urlProducer: (() -> String)? = null
	private var basicSocket: BasicSocket? = null
	private var onConnectionOpen: (suspend SocketControl.() -> Unit)? = null
	private var onConnectionClosed: (suspend SocketControl.() -> Unit)? = null
	private var onSocketStop: (suspend (SelfManagingSocket) -> Unit)? = null
	private var binaryBlock: (suspend SocketControl.(ByteArray) -> Unit)? = null
	private var textBlock: (suspend SocketControl.(String) -> Unit)? = null

	fun url(url: String): SocketBuilder {
		urlProducer = { url }
		return this
	}

	fun url(url: () -> String): SocketBuilder {
		urlProducer = url
		return this
	}

	fun socket(socket: BasicSocket): SocketBuilder {
		basicSocket = socket
		return this
	}

	fun onConnectionOpen(onOpen: (suspend SocketControl.() -> Unit)): SocketBuilder {
		onConnectionOpen = onOpen
		return this
	}

	fun onConnectionClosed(onClose: (suspend SocketControl.() -> Unit)): SocketBuilder {
		onConnectionClosed = onClose
		return this
	}

	fun onSocketStop(onStop: (suspend (SelfManagingSocket) -> Unit)): SocketBuilder {
		onSocketStop = onStop
		return this
	}

	fun onBinaryMsg(block: (suspend SocketControl.(ByteArray) -> Unit)): SocketBuilder {
		binaryBlock = block
		return this
	}

	fun onTextMsg(block: (suspend SocketControl.(String) -> Unit)): SocketBuilder {
		textBlock = block
		return this
	}

	fun build(): SelfManagingSocket {
		require(textBlock != null)
		require(urlProducer != null)
		val url = urlProducer ?: throw IllegalArgumentException("Url is missing")
		val block = textBlock ?: throw IllegalArgumentException("Msg handle is missing")
		val socket = basicSocket ?: BasicSocket()

		return SelfManagingSocket(
			name, socket, url, onConnectionOpen, onConnectionClosed, onSocketStop, binaryBlock, block
		)
	}
}

class SelfManagingSocket(
	private val name: String,
	private val socket: BasicSocket,
	private val urlProducer: () -> String,
	private val onConnectionOpen: (suspend SocketControl.() -> Unit)? = null,
	private val onConnectionClosed: (suspend SocketControl.() -> Unit)? = null,
	private val onSocketStop: (suspend (SelfManagingSocket) -> Unit)? = null,
	private val binaryBlock: (suspend SocketControl.(ByteArray) -> Unit)? = null,
	private val block: suspend SocketControl.(String) -> Unit,
) : SocketControl {

	private val activated = AtomicBoolean(false)
	private val retryTimer = RetryTimer(delay = TimeUnit.SECONDS.toMillis(2))

	// drop the oldest entry in cache
	private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	private var connectionJob: Job? = null
	private var session: WeakReference<SocketSession>? = null
	private var activeDeferred: Deferred<Unit>? = null

	fun start() {
		if (!activated.getAndSet(true)) {
			internalStart()
		}
	}

	fun activated() = activated.get()
	fun isRunning() = session?.get() != null

	fun stopOnNext() {
		activated.set(false)
	}

	override fun close() {
		stopOnNext()
		closeAndMaybeReconnect()
	}

	override fun closeAndMaybeReconnect() {
		val localSession = session?.get()
		if (localSession != null) {
			localSession.cancel()
		} else {
			activeDeferred?.cancel()
		}
	}

	override suspend fun send(msg: ByteArray): Boolean {
		session?.get()?.send(msg) ?: return false
		return true
	}

	override suspend fun send(msg: String): Boolean {
		session?.get()?.send(msg) ?: return false
		return true
	}

	private fun internalStart() {
		connectionJob = scope.launch {
			while (activated()) {
				val def = scope.async(start = CoroutineStart.LAZY) {
					socket.connect(urlProducer()) {
						session = WeakReference(this)
						onConnectionOpen?.invoke(this@SelfManagingSocket)
						this.handleMsg()
						onConnectionClosed?.invoke(this@SelfManagingSocket)
						session = null
					}
				}
				def.start()
				logMsg("has started")
				try {
					def.await()
				} catch (e: Exception) {
					logMsg("received an error: $e")
				}
				val nextDelay = retryTimer.getNextRetryDelay()
				if (activated()) {
					logMsg("has stopped, restart in ${nextDelay}ms")
				} else {
					logMsg("has stopped, wont restart")
				}
				delay(nextDelay)
			}
			onSocketStop?.invoke(this@SelfManagingSocket)
		}
	}

	private suspend fun SocketSession.handleMsg() {
		while (this.isActive()) {
			val result = when (val msg = this.receive()) {
				is SocketFrame.Binary -> binaryBlock?.invoke(this@SelfManagingSocket, msg.data) ?: continue
				is SocketFrame.Text -> block(this@SelfManagingSocket, msg.data)
				is SocketFrame.Close -> break
				else -> continue
			}
		}
	}

	private fun logMsg(msg: String) {
		println("[${name}] -> $msg")
	}

	companion object {
		fun builder(name: String) = SocketBuilder(name)
	}

}

interface SocketControl {
	fun close()
	fun closeAndMaybeReconnect()
	suspend fun send(msg: String): Boolean
	suspend fun send(msg: ByteArray): Boolean
}

