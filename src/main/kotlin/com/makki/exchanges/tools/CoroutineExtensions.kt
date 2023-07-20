package com.makki.exchanges.tools

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@Suppress("FunctionName")
fun <T> CachedStateSubject(overflow: BufferOverflow) = MutableSharedFlow<T>(1, 1, overflow)

@Suppress("FunctionName")
fun <T> FreshOnlySubject() = MutableSharedFlow<T>(0, 0)


suspend inline fun <T> asyncInContext(crossinline block: suspend () -> T): Deferred<T> {
	return withContext(coroutineContext) {
		return@withContext async {
			block()
		}
	}
}