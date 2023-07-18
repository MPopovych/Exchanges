package com.makki.exchanges.logging

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class LogLevel(val priority: Int) {
	Error(0),
	Warning(1),
	Info(2),
	Debug(3),
}

class LoggerBuilder internal constructor(private val callerName: String) {

	private var time: Boolean = true
	private var timePattern: String = "HH:mm:ss.SSS"
	private var logLevel: LogLevel = LogLevel.Info

	fun time(enabled: Boolean): LoggerBuilder = this.also { time = enabled }

	fun timeFormat(pattern: String): LoggerBuilder = this.also {
		time = true
		timePattern = pattern
	}

	fun level(level: LogLevel): LoggerBuilder = this.also { logLevel = level }

	fun build(): Logger {
		return Logger(callerName, logLevel, time, timePattern)
	}
}

class Logger internal constructor(
	private val callerName: String,
	private val logLevel: LogLevel,
	private val time: Boolean,
	timePattern: String,
) {

	private val formatter = DateTimeFormatter.ofPattern(timePattern)

	fun printDebug(msg: Any, caller: String? = null) {
		println(getMsgForLevel(msg, LogLevel.Debug, caller) ?: return)
	}

	fun printInfo(msg: Any, caller: String? = null) {
		println(getMsgForLevel(msg, LogLevel.Info, caller) ?: return)
	}

	fun printWarning(msg: Any, caller: String? = null) {
		println(getMsgForLevel(msg, LogLevel.Warning, caller) ?: return)
	}

	fun printError(msg: Any, caller: String? = null) {
		println(getMsgForLevel(msg, LogLevel.Error, caller) ?: return)
	}

	private fun getMsgForLevel(msg: Any, level: LogLevel, caller: String?): String? {
		if (level.priority > logLevel.priority) return null
		return formatToLevel(formatContent(msg, caller), level)
	}

	private fun formatContent(msg: Any, caller: String?): String {
		val time = if (time) {
			"${formatter.format(LocalDateTime.now())} -\t"
		} else {
			""
		}

		return "${time}[${caller ?: callerName}]:\t$msg"
	}

	private fun formatToLevel(msg: String, level: LogLevel): String {
		return when (level) {
			LogLevel.Error -> msg.wrapColor(ColorCodes.ANSI_RED)
			LogLevel.Warning -> msg.wrapColor(ColorCodes.ANSI_YELLOW)
			LogLevel.Info -> msg.wrapColor(ColorCodes.ANSI_WHITE)
			LogLevel.Debug -> msg.wrapColor(ColorCodes.ANSI_CYAN)
		}
	}

	private fun String.wrapColor(color: String): String {
		return "$color$this${ColorCodes.ANSI_RESET}".replace("\n", "${ColorCodes.ANSI_RESET}\n$color")
	}

}

fun Any.logger(): Logger {
	return this.loggerBuilder().build()
}

fun Any.loggerBuilder(): LoggerBuilder {
	return LoggerBuilder(this::class.simpleName ?: this::class.java.simpleName)
}