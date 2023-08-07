package com.makki.exchanges.abtractions

import com.makki.exchanges.tools.StateTree
import kotlinx.serialization.json.JsonElement

interface StateObservable {
	fun stateTree(): StateTree
}

interface StateFormat {
	fun format(): String
}

interface StateJson {
	fun toJson(): JsonElement
}