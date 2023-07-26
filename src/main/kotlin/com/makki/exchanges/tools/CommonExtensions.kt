package com.makki.exchanges.tools

import java.math.BigDecimal
import java.math.RoundingMode

fun String.inIgC(value: String) = this.contains(value, true)

fun String.eqIgC(value: String) = this.equals(value, true)


fun produceError(): Nothing = throw IllegalStateException("Logic failure")

@Throws
fun String.findPrecision(): Int {
	val indexOfDot = this.indexOf('.')
	if (indexOfDot == -1) throw IllegalStateException("No precision in string: $this")

	val indexOfOne = this.substring(indexOfDot + 1).indexOfFirst { c -> c != '0' }
	return if (indexOfOne == -1) {
		-indexOfDot + 1
	} else {
		indexOfOne + 1
	}
}

fun String.ellipsis(size: Int): String {
	require(size > 0)
	if (this.length > size) {
		return "${this.take(size).trim()}..."
	}
	return this
}

fun String.ellipsisSingleLine(size: Int): String {
	return this.replace("\n", " ").ellipsis(size)
}

fun Number.roundToDec(precision: Int, rounding: RoundingMode = RoundingMode.HALF_EVEN): BigDecimal {
	return this.toDouble().toBigDecimal().setScale(precision, rounding)
}

fun Number.roundToDecString(decimals: Int, rounding: RoundingMode = RoundingMode.HALF_EVEN): String {
	return roundToDec(decimals, rounding)
		.toPlainString()
		.removeTrailingZeroes()
}

fun String.removeTrailingZeroes(): String {
	if (contains('.')) {
		return this.trimEnd('0').trimEnd('.')
	}
	return this
}