package com.makki.exchanges.nontesting

import com.makki.exchanges.logging.LogLevel
import com.makki.exchanges.logging.loggerBuilder
import kotlinx.coroutines.runBlocking

object TestLogger {
	val logger = loggerBuilder().level(LogLevel.Debug).time(false).build()
}

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
	val env = System.getenv("asyncTestSecure")

	if (env.isNullOrBlank() || !checkContainsSeparated(env, password)) {
		TestLogger.logger.printWarning("Skipped test with key $password for ${block::class.java.enclosingMethod.name}")
		return
	}

	asyncTest(block)
}

private fun checkContainsSeparated(variable: String, password: String): Boolean {
	return password in variable.split(",").toSet()
}