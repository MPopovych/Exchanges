package com.makki.exchanges.tools

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean


/**
 * SharedResultMutex v3
 * 1. to prevent ddos
 * 2. to prevent multiple parallel actions by id
 *
 * Executes an action, stores it by key
 * If a second action with the same key is invoked while first action is executing, the second action is skipped
 * and subscribes to a result of the first one
 */
class SharedResult<T : Any> {
	private val callbacks = HashMap<String, PendingJob<T>>() // thread secure by mutex
	private val mapLock = Mutex()

	suspend fun shareResultByKey(
		key: String,
		action: suspend () -> T,
	): T {
		val job: PendingJob<T> = mapLock.withLock {
			val existing = callbacks[key]
			if (existing == null) {
				val newJob = PendingJob(key, action, this)
				callbacks[key] = newJob
				return@withLock newJob
			} else {
				return@withLock existing
			}
		}

		return job.join()
	}

	private suspend fun remove(job: PendingJob<*>) {
		mapLock.withLock {
			callbacks.remove(job.key)
		}
	}

	private class PendingJob<T : Any>(
		val key: String,
		private val action: suspend () -> T,
		private val parent: SharedResult<T>,
	) {
		private val started = AtomicBoolean(false)
		private val task = CompletableDeferred<T>()

		suspend fun join(): T {
			if (!started.getAndSet(true)) {
				task.complete(action())
			}
			val result = task.await()
			parent.remove(this)
			return result
		}
	}

}

