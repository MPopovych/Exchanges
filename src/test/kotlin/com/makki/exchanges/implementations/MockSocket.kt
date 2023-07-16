package com.makki.exchanges.implementations

import com.makki.exchanges.tools.FreshOnlySubject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take

class MockSocket(val handler: suspend () -> String) : BasicSocket() {

	private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	override suspend fun connect(url: String, block: suspend SocketSession.() -> Unit) {
		val flow = FreshOnlySubject<String>()
		val job = scope.launch {
			while (isActive) {
				flow.emit(handler())
				delay(1000)
			}
		}
		return block.invoke(MockSession(job, flow.asSharedFlow()))
	}

}

class MockSession(private val job: Job, private val readFlow: SharedFlow<String>) : SocketSession {

	override suspend fun receive(): SocketFrame {
		return SocketFrame.Text(readFlow.take(1).single())
	}

	override suspend fun send(text: String) {}

	override suspend fun send(byteArray: ByteArray) {}

	override fun cancel(e: CancellationException?) {
		job.cancel(e)
	}

	override fun isActive(): Boolean {
		return job.isActive
	}
}