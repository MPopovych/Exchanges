package com.makki.exchanges.tools

import com.makki.exchanges.common.Result

/**
 * Pure throughput on M1 mac is around 16_500_000 ±20% (for 3 lock) ops/sec
 * ~5333858 ops/sec for 100 locks
 * Which should not be a heavy load compared to weight of http requests and risks of ddos
 */
class RateLimiterWeighted(
	private val intervalMs: Long,
	private val weightLimit: Float,
) {

	private val active = weightLimit > 0
	private val accounting = ArrayList<Pair<Long, Float>>()

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
			accountFor(weight)
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
			accountFor(weight)
		}
		return block()
	}

	@Synchronized
	private fun accountFor(weight: Float) {
		accounting.add(Pair(System.currentTimeMillis(), weight))
	}

	@Synchronized
	private fun countAndClearAccount(): Float {
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
		return sum
	}


	data object Rejection
}