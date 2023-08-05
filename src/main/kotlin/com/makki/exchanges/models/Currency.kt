package com.makki.exchanges.models

@JvmInline
value class Currency private constructor(val name: String) {
	companion object {
		operator fun invoke(n: String) = Currency(n.uppercase())
	}
}