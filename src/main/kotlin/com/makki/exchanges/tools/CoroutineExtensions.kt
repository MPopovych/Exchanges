package com.makki.exchanges.tools

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

@Suppress("FunctionName")
fun <T> CachedSubject(overflow: BufferOverflow) = MutableSharedFlow<T>(1, 1, overflow)

@Suppress("FunctionName")
fun <T> FreshOnlySubject() = MutableSharedFlow<T>(0, 0)