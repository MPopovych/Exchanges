package com.makki.exchanges.logging

fun Any.extLog(msg: Any, color: String = ColorCodes.ANSI_WHITE) {
	println("[${this::class.simpleName}] -> $msg".wrapColor(color))
}

fun Any.extLogGray(msg: Any) {
	extLog(msg, ColorCodes.ANSI_GRAY)
}


fun Any.extLogRed(msg: Any) {
	extLog(msg, ColorCodes.ANSI_RED)
}

fun Any.extLogGreen(msg: Any) {
	extLog(msg, ColorCodes.ANSI_GREEN)
}

fun Any.extLogBlue(msg: Any) {
	extLog(msg, ColorCodes.ANSI_BLUE)
}

fun Any.extLogYellow(msg: Any) {
	extLog(msg, ColorCodes.ANSI_YELLOW)
}

fun Any.extLogPurple(msg: Any) {
	extLog(msg, ColorCodes.ANSI_PURPLE)
}

private fun String.wrapColor(color: String): String {
	return "$color$this${ColorCodes.ANSI_RESET}".replace("\n", "${ColorCodes.ANSI_RESET}\n$color")
}