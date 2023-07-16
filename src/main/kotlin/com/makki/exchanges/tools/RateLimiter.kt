package com.makki.exchanges.tools

import kotlin.math.max

class RateLimiter(
	private val interval: Long,
	private val rate: Int,
) {

	private var currentCycleStart: Long = System.currentTimeMillis()
	private var currentCycleEnd: Long = System.currentTimeMillis() + interval
	private var tickCurrentCount = 0
	private var tickOveruseCount = 0

	private fun nextCycle() {
		var subtractCount = rate
		val now = System.currentTimeMillis()
		if (now > currentCycleEnd + interval) { // skipped at-least 1 cycle
			val passed = now - currentCycleEnd
			val iterations = passed / interval
			subtractCount = rate * iterations.toInt()
		}

		tickCurrentCount = tickOveruseCount
		tickOveruseCount = max(0, tickOveruseCount - subtractCount)

		currentCycleStart = now
		currentCycleEnd = currentCycleStart + interval
	}

	@Synchronized
	fun tryAcquire(): Boolean {
		if (System.currentTimeMillis() > currentCycleEnd) {
			nextCycle()
		}

		if (tickCurrentCount >= rate) {
			return false
		}

		tickCurrentCount++
		return true
	}

	@Synchronized
	fun forceAcquire() {
		if (tryAcquire()) {
			return
		}
		tickOveruseCount++
	}

}