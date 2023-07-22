package com.makki.exchanges.wrapper

class ApiConfig(
	val safeGuard: Boolean = true, // post protection
	val weightLimit: Float? = null, // rate limiter
	val intervalMs: Long? = null, // rate limiter
) {
}