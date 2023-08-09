package com.makki.exchanges.tools

import java.util.concurrent.TimeUnit
import kotlin.math.min

class RetryTimer(
	private val delayMs: Long,
	private val maxMultiplier: Int = 5,
	private val failInterval: Long = delayMs * 5,
) {

	private var lastRetry = 0L
	private var lastDelay = 0L
	private var sequentRetryCount = 1

	init {
		require(maxMultiplier > 0)
		require(delayMs > TimeUnit.MILLISECONDS.toMillis(100))
	}

	fun getNextRetryDelay(): Long {
		if (lastRetry + delayMs * sequentRetryCount + failInterval > System.currentTimeMillis()) {
			sequentRetryCount++
		} else {
			sequentRetryCount = 1
		}
		lastRetry = System.currentTimeMillis()
		lastDelay = delayMs * min(sequentRetryCount, maxMultiplier)
		return lastDelay
	}

	fun reset() {
		sequentRetryCount = 1
		lastDelay = delayMs
	}

}