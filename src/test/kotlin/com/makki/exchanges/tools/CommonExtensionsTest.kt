package com.makki.exchanges.tools

import org.junit.jupiter.api.assertThrows
import kotlin.Exception
import kotlin.test.Test

class CommonExtensionsTest {

	@Test
	fun testFindPrecision() {
		assert("1.0".findPrecision() == 0)
		assert("10.000".findPrecision() == -1)
		assert("0.1".findPrecision() == 1)
		assert("0.00001".findPrecision() == 5)

		assertThrows<Exception> {
			"NaN".findPrecision()
		}
	}

	@Test
	fun testProduceError() {
		assertThrows<Exception> { produceError() }
	}

	@Test
	fun testRounding() {
		val value = 123.10940200

		assert(value.roundToDec(-2).toDouble() == 100.0)
		assert(value.roundToDec(-1).toDouble() == 120.0)
		assert(value.roundToDec(0).toDouble() == 123.0)
		assert(value.roundToDec(1).toDouble() == 123.1)
		assert(value.roundToDec(2).toDouble() == 123.11)
		assert(value.roundToDec(3).toDouble() == 123.109)

		assert(value.roundToDecString(0) == "123")
		assert(value.roundToDecString(1) == "123.1")
		assert(value.roundToDecString(3) == "123.109")
		assert(value.roundToDecString(8) == "123.109402")


		assert("123000".removeTrailingZeroes() == "123000")
		assert("123.000".removeTrailingZeroes() == "123")
	}

}