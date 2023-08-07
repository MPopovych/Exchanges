package com.makki.exchanges.models

import com.makki.exchanges.tools.eqIgC

@JvmInline
value class Currency private constructor(private val name: String) {
	companion object {
		operator fun invoke(n: String) = Currency(n.uppercase())
	}

	fun upName() = name
	fun lowName() = name.lowercase()
	fun eq(other: String) = name.eqIgC(other)
}