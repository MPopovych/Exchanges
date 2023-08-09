package com.makki.exchanges.common.serializers

import kotlinx.serialization.json.Json

object GlobalJson {
	val json = Json {
		ignoreUnknownKeys = true
	}
}