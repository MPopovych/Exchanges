package com.makki.exchanges.tools

import java.math.BigDecimal
import java.math.RoundingMode

fun Number.roundToDec(precision: Int, rounding: RoundingMode = RoundingMode.HALF_EVEN): BigDecimal {
	return this.toDouble().toBigDecimal().setScale(precision, rounding)
}

fun Number.roundToDecString(decimals: Int, rounding: RoundingMode = RoundingMode.HALF_EVEN): String {
	return roundToDec(decimals, rounding)
		.trimStr()
}

fun BigDecimal.trimStr(): String = this.stripTrailingZeros().toPlainString()

fun BigDecimal.isZero(): Boolean = this.signum() == 0