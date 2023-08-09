package com.makki.exchanges.tools

import com.makki.exchanges.common.Result

/**
 * Pure throughput on M1 mac is around 16_500_000 ±20% (for 3 lock) ops/sec
 * ~5_333_858 ops/sec for 100 locks
 * Which should not be a heavy load compared to weight of http requests and risks of ddos
 */
class RateLimiterWeighted(
	val intervalMs: Long,
	val weightLimit: Float,
) {

	private val active = weightLimit > 0
	private val accounting = ArrayList<Pair<Long, Float>>()

	fun isActive() = active

	/**
	 * Used for requests eligible for rate-limiting
	 * Ex.: a balance refresh or checking the state of an order
	 */
	inline fun <T> tryRun(weight: Float, block: () -> T): Result<T, Rejection> {
		if (!isActive()) return Result.Ok(block())

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
	inline fun <T> forceRun(weight: Float, block: () -> T): T {
		// add weight to current window
		if (isActive()) {
			accountFor(weight)
		}
		return block()
	}

	@Synchronized
	fun accountFor(weight: Float) {
		accounting.add(Pair(System.currentTimeMillis(), weight))
	}

	@Synchronized
	fun countAndClearAccount(): Float {
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


	object Rejection
}