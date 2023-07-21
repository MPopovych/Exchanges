package com.makki.exchanges.implementations

import com.makki.exchanges.abtractions.SocketApi
import com.makki.exchanges.logging.LogLevel
import com.makki.exchanges.logging.loggerBuilder
import com.makki.exchanges.tools.RetryTimer
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class SelfManagingSocket(
	private val name: String,
	private val socket: SocketApi,
	private val urlProducer: () -> String,
	private val onConnectionOpen: (suspend SocketControl.() -> Unit)? = null,
	private val onConnectionClosed: (suspend SocketControl.() -> Unit)? = null,
	private val onConnectionRejected: (suspend SocketControl.() -> Unit)? = null,
	private val onSocketStop: (suspend (SelfManagingSocket) -> Unit)? = null,
	private val binaryBlock: (suspend SocketControl.(ByteArray) -> Unit)? = null,
	private val block: suspend SocketControl.(String) -> Unit,
) : SocketControl {

	private val activated = AtomicBoolean(false)
	private val retryTimer = RetryTimer(delayMs = TimeUnit.SECONDS.toMillis(2))

	// drop the oldest entry in cache
	private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	private var connectionJob: Job? = null
	private var session: WeakReference<SocketSession>? = null
	private var activeDeferred: Deferred<Unit>? = null

	private val logger = loggerBuilder().level(LogLevel.Debug).build()

	/**
	 * Cannot be started if already is
	 */
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
						logger.printInfoPositive("$name connected")
						session = WeakReference(this)
						onConnectionOpen?.invoke(this@SelfManagingSocket)
						this.handleMsg()
						onConnectionClosed?.invoke(this@SelfManagingSocket)
						session = null
					}
				}
				def.start()
				logger.printDebug("$name trying to connect")
				try {
					def.await()
				} catch (e: Exception) {
					onConnectionRejected?.invoke(this@SelfManagingSocket)
					logger.printWarning("$name rejected, received an error: $e")
				}
				val nextDelay = retryTimer.getNextRetryDelay()
				if (activated()) {
					logger.printWarning("$name stopped, restart in ${nextDelay}ms")
				} else {
					logger.printError("$name stopped, wont restart")
				}
				delay(nextDelay)
			}
			onSocketStop?.invoke(this@SelfManagingSocket)
		}
	}

	private suspend fun SocketSession.handleMsg() {
		while (this.isActive()) {
			when (val msg = this.receive()) {
				is SocketFrame.Binary -> binaryBlock?.invoke(this@SelfManagingSocket, msg.data) ?: continue
				is SocketFrame.Text -> block(this@SelfManagingSocket, msg.data)
				is SocketFrame.Close -> {
					logger.printWarning("$name socket closing: $msg")
					break
				}

				else -> {
					logger.printDebug { "sending sub: ${msg.toString().replace("\n", " ")}" }
					continue
				}
			}
		}
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

class SocketBuilder(private val name: String) {
	private var urlProducer: (() -> String)? = null
	private var basicSocket: SocketApi? = null
	private var onConnectionOpen: (suspend SocketControl.() -> Unit)? = null
	private var onConnectionClosed: (suspend SocketControl.() -> Unit)? = null
	private var onConnectionRejected: (suspend SocketControl.() -> Unit)? = null
	private var onSocketStop: (suspend (SelfManagingSocket) -> Unit)? = null
	private var binaryBlock: (suspend SocketControl.(ByteArray) -> Unit)? = null
	private var textBlock: (suspend SocketControl.(String) -> Unit)? = null

	fun url(url: String) = this.also {
		urlProducer = { url }
	}

	fun url(url: () -> String) = this.also {
		urlProducer = url
	}

	fun socket(socket: SocketApi) = this.also {
		basicSocket = socket
	}

	fun onConnectionOpen(onOpen: (suspend SocketControl.() -> Unit)) = this.also {
		onConnectionOpen = onOpen
	}

	fun onConnectionClosed(onClose: (suspend SocketControl.() -> Unit)) = this.also {
		onConnectionClosed = onClose
	}

	fun onConnectionRejected(onReject: (suspend SocketControl.() -> Unit)) = this.also {
		onConnectionRejected = onReject
	}

	fun onSocketStop(onStop: (suspend (SelfManagingSocket) -> Unit)) = this.also {
		onSocketStop = onStop
	}

	fun onBinaryMsg(block: (suspend SocketControl.(ByteArray) -> Unit)) = this.also {
		binaryBlock = block
	}

	fun onTextMsg(block: (suspend SocketControl.(String) -> Unit)) = this.also {
		textBlock = block
	}

	fun build(): SelfManagingSocket {
		require(textBlock != null)
		require(urlProducer != null)
		val url = urlProducer ?: throw IllegalArgumentException("Url is missing")
		val block = textBlock ?: throw IllegalArgumentException("Msg handle is missing")
		val socket = basicSocket ?: BasicSocket()

		return SelfManagingSocket(
			name,
			socket,
			url,
			onConnectionOpen,
			onConnectionClosed,
			onConnectionRejected,
			onSocketStop,
			binaryBlock,
			block
		)
	}
}