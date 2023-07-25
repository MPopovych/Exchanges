package com.makki.exchanges.abtractions

import com.makki.exchanges.tools.StateObserver

interface StateObservable {
	fun state(): StateObserver
}