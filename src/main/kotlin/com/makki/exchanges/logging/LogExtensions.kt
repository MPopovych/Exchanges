package com.makki.exchanges.logging

fun Any.printLog(msg: String) {
	println("[${this::class.simpleName}] -> $msg")
}