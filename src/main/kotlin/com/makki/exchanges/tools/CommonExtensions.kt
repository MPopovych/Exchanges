package com.makki.exchanges.tools

fun String.inC(value: String) = this.contains(value, true)

fun produceError(): Nothing = throw IllegalStateException("Logic failure")