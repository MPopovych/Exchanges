package com.makki.exchanges.wrapper

class SafeGuardConfig(
	val allowOrderCreation: Boolean = true, // post protection
	val weightLimit: Float? = null, // rate limiter
	val intervalMs: Long? = null, // rate limiter
) {
}