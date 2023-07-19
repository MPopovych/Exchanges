package com.makki.exchanges.wrapper

interface ErrorTags {
	interface Persisting : ErrorTags
	interface Temporary : ErrorTags
	interface LogicMiss : ErrorTags
}