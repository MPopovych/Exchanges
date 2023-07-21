package com.makki.exchanges.tools

import com.makki.exchanges.nontesting.TestLogger
import com.makki.exchanges.nontesting.asyncTest
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class RateLimiterWeightedTest {

	@Test
	fun testRejectionsSmall() = asyncTest {
		val limiter = RateLimiterWeighted(100, 10f)
		val start = System.currentTimeMillis()
		var passCount = 0
		var rejected = 0
		while (System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(1) < start) {
			val r = limiter.tryRun(1f) {
				// doing Nothing
				val t = 2 + 2
				t.toShort()
			}
			if (r.isOk()) {
				passCount++
			} else {
				rejected++
			}
		}
		TestLogger.logger.printDebug("passCount: $passCount, rejected: $rejected")
		assert(passCount in 95..105) { "passCount: $passCount" }
	}

	@Test
	fun testRejections() = asyncTest {
		val limiter = RateLimiterWeighted(100, 1f)
		val start = System.currentTimeMillis()
		var passCount = 0
		var rejected = 0
		while (System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(1) < start) {
			val r = limiter.tryRun(1f) {
				// doing Nothing
				val t = 2 + 2
				t.toShort()
			}
			if (r.isOk()) {
				passCount++
			} else {
				rejected++
			}
		}
		TestLogger.logger.printDebug("passCount: $passCount, rejected: $rejected")
		assert(passCount in 9..11) { "PassCount: $passCount" }
	}

	@Test
	fun testForceRun() = asyncTest {
		val limiter = RateLimiterWeighted(100, 1f)
		val start = System.currentTimeMillis()
		var callCount = 0
		var passCount = 0
		while (System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(1) < start) {
			callCount++
			limiter.forceRun(10f) {
				// doing Nothing
				val t = 2 + 2
				t.toShort()
				delay(10)
				passCount++
			}
		}
		TestLogger.logger.printDebug("passCount: $passCount, rejected: ${passCount - callCount}")
		assert(callCount > 10) { "PassCount: $passCount" }
		assertEquals(callCount, passCount, "passCount: ${passCount}, callCount: $callCount")
	}

}