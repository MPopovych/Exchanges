package com.makki.exchanges

import kotlinx.coroutines.runBlocking

/**
 * Extension for running suspended tests and to return Unit signature (required by @Test annotation)
 */
fun asyncTest(block: suspend () -> Unit) {
	runBlocking { block() }
}

/**
 * Safe-guard for post requests, requires a specific env variable
 */
fun asyncTestSecure(password: String, block: suspend () -> Unit) {
	val env = System.getenv("asyncTestSecure") ?: return
	if (!checkContainsSeparated(env, password)) return

	asyncTest(block)
}

private fun checkContainsSeparated(variable: String, password: String): Boolean {
	return password in variable.split(",").toSet()
}