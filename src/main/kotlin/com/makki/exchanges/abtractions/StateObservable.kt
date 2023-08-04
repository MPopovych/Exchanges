package com.makki.exchanges.abtractions

import com.makki.exchanges.tools.StateTree

interface StateObservable {
	fun state(): StateTree
}