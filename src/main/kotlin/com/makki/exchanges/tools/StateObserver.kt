package com.makki.exchanges.tools

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

class StateObserver {
	private val cache = ConcurrentHashMap<String, (() -> Any?)>()
	private val state = CachedStateSubject<Map<String, Any?>>(overflow = BufferOverflow.DROP_OLDEST)

	fun observe() = state.asSharedFlow()

	fun track(name: String, block: (() -> Any)): StateObserver {
		cache[name] = block
		return this
	}

	suspend fun update() {
		state.emit(get())
	}

	fun merge(key: String, other: StateObserver): StateObserver {
		track(key) { other.get() }
		return this
	}

	fun get() = HashMap(cache.mapValues { it.value.invoke() })
}