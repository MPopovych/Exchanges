package com.makki.exchanges.common

import kotlinx.serialization.Serializable

/**
 * This is a utility wrapper for strictly typing the arguments and keys in caches
 */
@Serializable
@JvmInline
value class UniqueKey(val value: String)
