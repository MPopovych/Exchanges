package com.makki.exchanges.logging

import kotlin.test.Test

class LoggerTest {

	@Test
	fun testPrintGlobal() {
		extLogDebug("debug log")
		extLogInfo("info log")
		extLogWarning("warning log")
		extLogError("error log")
	}

	@Test
	fun testPrintDebug() {
		val logger = loggerBuilder().level(LogLevel.Debug).build()
		logger.printDebug("debug log")
		logger.printInfo("info log")
		logger.printWarning("warning log")
		logger.printError("error log")
	}

	@Test
	fun testPrintInfo() {
		val logger = loggerBuilder().level(LogLevel.Info).build()
		logger.printDebug("debug log")
		logger.printInfo("info log")
		logger.printWarning("warning log")
		logger.printError("error log")
	}

	@Test
	fun testPrintWarning() {
		val logger = loggerBuilder().level(LogLevel.Warning).build()
		logger.printDebug("debug log")
		logger.printInfo("info log")
		logger.printWarning("warning log")
		logger.printError("error log")
	}

	@Test
	fun testPrintError() {
		val logger = loggerBuilder().level(LogLevel.Error).build()
		logger.printDebug("debug log")
		logger.printInfo("info log")
		logger.printWarning("warning log")
		logger.printError("error log")
	}
}