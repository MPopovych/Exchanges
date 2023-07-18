package com.makki.exchanges.logging

object GlobalLogger {
	var logger = defaultLogger()
}

fun Any.extLogError(msg: Any) = GlobalLogger.logger.printError(msg, callerName())

fun Any.extLogWarning(msg: Any) = GlobalLogger.logger.printWarning(msg, callerName())

fun Any.extLogInfo(msg: Any) = GlobalLogger.logger.printInfo(msg, callerName())

fun Any.extLogDebug(msg: Any) = GlobalLogger.logger.printDebug(msg, callerName())

private fun Any.callerName() = this::class.simpleName ?: this::class.java.simpleName