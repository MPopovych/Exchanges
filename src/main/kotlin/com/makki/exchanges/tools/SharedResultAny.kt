package com.makki.exchanges.tools

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass


/**
 * SharedResultMutex v3
 * 1. to prevent ddos
 * 2. to prevent multiple parallel actions by id
 *
 * Executes an action, stores it by key
 * If a second action with the same key is invoked while first action is executing, the second action is skipped
 * and subscribes to a result of the first one
 */
class SharedResultAny {
	private val callbacks = HashMap<KClass<*>, HashMap<String, PendingJob<*>>>()
	private val mapLock = Mutex()

	suspend inline fun <reified T : Any> shareResultByKey(
		key: String,
		noinline action: suspend () -> T,
	): T {
		return shareResultByKey(key, T::class, action)
	}

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any> shareResultByKey(
		key: String,
		klass: KClass<T>,
		action: suspend () -> T,
	): T {
		val job: PendingJob<T> = mapLock.withLock {
			val klassMap = callbacks.getOrPut(klass) { HashMap() }
			val existing = klassMap[key]
			if (existing == null) {
				val newJob = PendingJob(key, klass, action, this)
				klassMap[key] = newJob
				return@withLock newJob
			} else if (existing.klass == klass) {
				// if KClasses are matching - it's the same type
				return@withLock existing as? PendingJob<T> ?: throw IllegalStateException("Mismatch of types")
			} else {
				throw IllegalStateException("Missing type of types")
			}
		}

		return job.join()
	}

	private suspend fun remove(job: PendingJob<*>) {
		mapLock.withLock {
			callbacks[job.klass]?.remove(job.key)
		}
	}

	private class PendingJob<T : Any>(
		val key: String,
		val klass: KClass<T>,
		val action: suspend () -> T,
		private val parent: SharedResultAny,
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


