package com.makki.exchanges.tools

import com.makki.exchanges.common.Result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList

/**
 * Pure throughput on M1 mac is around ±15648826 ops/sec
 * Which should not be a heavy load compared to weight of http requests and risks of ddos
 */
class RateLimiterWeighted(
	private val intervalMs: Long,
	private val weightLimit: Float,
) {

	private val active = weightLimit > 0
	private val accounting = LinkedList<Pair<Long, Float>>()
	private val accountMutex = Mutex()

	/**
	 * Used for requests eligible for rate-limiting
	 * Ex.: a balance refresh or checking the state of an order
	 */
	suspend fun <T> tryRun(weight: Float, block: suspend () -> T): Result<T, Rejection> {
		require(weight >= 0)

		if (!active) return Result.Ok(block())

		return if (countAndClearAccount() >= weightLimit) {
			Result.Error(Rejection)
		} else {
			addAccount(weight)
			Result.Ok(block())
		}
	}

	/**
	 * Used for requests that NEED to be executed, although they are accounted
	 * Ex.: an urgent cancel order
	 */
	suspend fun <T> forceRun(weight: Float, block: suspend () -> T): T {
		require(weight >= 0)
		// add weight to current window
		if (active) {
			addAccount(weight)
		}
		return block()
	}

	private suspend fun addAccount(weight: Float) = accountMutex.withLock {
		accounting.add(Pair(System.currentTimeMillis(), weight))
	}

	private suspend fun countAndClearAccount(): Float = accountMutex.withLock {
		var sum = 0f
		val now = System.currentTimeMillis()
		val iterator = accounting.iterator()
		while (iterator.hasNext()) {
			val next = iterator.next()
			if (next.first + intervalMs < now) {
				iterator.remove()
			} else {
				sum += next.second
			}
		}
		return@withLock sum
	}


	data object Rejection
}