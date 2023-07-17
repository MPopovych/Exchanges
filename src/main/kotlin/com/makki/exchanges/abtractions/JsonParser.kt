package com.makki.exchanges.abtractions

import kotlinx.serialization.json.Json

/**
 * Abstracted json parse, default internally uses kotlinx
 *
 * Reified extension function extract the klass used by most libraries
 */
interface JsonParser {
	companion object {
		val default = Json { ignoreUnknownKeys = true }
	}
}
