package com.makki.exchanges.tools

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun String.inIgC(value: String) = this.contains(value, true)

fun String.eqIgC(value: String) = this.equals(value, true)

fun String.urlEncodeUTF8(): String {
	return URLEncoder.encode(this, StandardCharsets.UTF_8)
}

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

fun String.removeTrailingZeroes(): String {
	if (contains('.')) {
		return this.trimEnd('0').trimEnd('.')
	}
	return this
}