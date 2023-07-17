package com.makki.exchanges.logging

fun Any.printLog(msg: Any, color: String = ColorCodes.ANSI_WHITE) {
	println("[${this::class.simpleName}] -> $msg".wrapColor(color))
}

fun Any.printLogGray(msg: Any) {
	printLog(msg, ColorCodes.ANSI_GRAY)
}


fun Any.printLogRed(msg: Any) {
	printLog(msg, ColorCodes.ANSI_RED)
}

fun Any.printLogGreen(msg: Any) {
	printLog(msg, ColorCodes.ANSI_GREEN)
}

fun Any.printLogBlue(msg: Any) {
	printLog(msg, ColorCodes.ANSI_BLUE)
}

fun Any.printLogYellow(msg: Any) {
	printLog(msg, ColorCodes.ANSI_YELLOW)
}

fun Any.printLogPurple(msg: Any) {
	printLog(msg, ColorCodes.ANSI_PURPLE)
}

private fun String.wrapColor(color: String): String {
	return "$color$this${ColorCodes.ANSI_RESET}".replace("\n", "${ColorCodes.ANSI_RESET}\n$color")
}