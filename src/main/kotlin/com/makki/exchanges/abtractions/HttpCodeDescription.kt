package com.makki.exchanges.abtractions


sealed interface HttpErrorGroups {
	interface Persisting : HttpErrorGroups
	interface Temporary : HttpErrorGroups
	interface Ambiguous : HttpErrorGroups
}

sealed class HttpCodeDescription(val codeRange: IntRange) {
	object Ok : HttpCodeDescription(200..299), HttpErrorGroups.Ambiguous

	object BadRequest : HttpCodeDescription(400), HttpErrorGroups.Persisting
	object Unauthorized : HttpCodeDescription(401), HttpErrorGroups.Persisting
	object Forbidden : HttpCodeDescription(403), HttpErrorGroups.Persisting
	object NotFound : HttpCodeDescription(404), HttpErrorGroups.Persisting
	object MethodNotAllowed : HttpCodeDescription(405), HttpErrorGroups.Persisting
	object NotAcceptable : HttpCodeDescription(406), HttpErrorGroups.Persisting

	object RequestTimeout : HttpCodeDescription(408), HttpErrorGroups.Temporary
	object Other400 : HttpCodeDescription(409..428), HttpErrorGroups.Temporary
	object TooManyRequests : HttpCodeDescription(429), HttpErrorGroups.Temporary

	object InternalServerError : HttpCodeDescription(500), HttpErrorGroups.Ambiguous
	object NotImplemented : HttpCodeDescription(501), HttpErrorGroups.Persisting
	object Temporary500Error : HttpCodeDescription(502..504), HttpErrorGroups.Temporary

	data class Undefined(val code: Int) : HttpCodeDescription(1000..1000), HttpErrorGroups.Ambiguous

	constructor(value: Int) : this(value..value)

	companion object {
		private fun values() = HttpCodeDescription::class.sealedSubclasses.mapNotNull { it.objectInstance }
		fun findError(code: Int): HttpCodeDescription {
			return values().find { code in it.codeRange } ?: Undefined(code)
		}
	}

	fun isPersisting() = this is HttpErrorGroups.Persisting
	fun isTemporary() = this is HttpErrorGroups.Temporary
	fun isAmbiguous() = this is HttpErrorGroups.Ambiguous
	fun isOk() = this is Ok
}