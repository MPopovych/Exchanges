package com.makki.exchanges

object TestResourceLoader {
	fun loadText(path: String): String {
		return this::class.java.getResourceAsStream(path)?.bufferedReader()?.readText()?.also {
			if (it.isBlank()) throw IllegalStateException("Empty resource")
		} ?: throw IllegalStateException("Failed to read resource with path: $path")
	}
}