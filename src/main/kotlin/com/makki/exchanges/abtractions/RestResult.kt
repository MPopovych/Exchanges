package com.makki.exchanges.abtractions

import com.makki.exchanges.tools.ellipsisSingleLine

sealed interface RestResult<T, E> {
	data class Ok<T, E>(val data: T) : RestResult<T, E>

	sealed interface Error<T, E> : RestResult<T, E>
	data class RestError<T, E>(val error: E) : Error<T, E>
	data class HttpError<T, E>(val code: Int) : Error<T, E>
	data class ParseError<T, E>(val exception: Exception, val json: String) : Error<T, E>
	data class ConnectionError<T, E>(val exception: Exception) : Error<T, E>

	fun <M : Any> map(block: (T) -> M): RestResult<M, E> {
		return when (this) {
			is ConnectionError -> ConnectionError(exception)
			is HttpError -> HttpError(code)
			is ParseError -> ParseError(exception, json)
			is RestError -> RestError(error)
			is Ok -> Ok(block(data))
		}
	}

	fun <M : Any> mapRestError(block: (E) -> M): RestResult<T, M> {
		return when (this) {
			is ConnectionError -> ConnectionError(exception)
			is HttpError -> HttpError(code)
			is ParseError -> ParseError(exception, json)
			is RestError -> RestError(block(error))
			is Ok -> Ok(data)
		}
	}

	fun mapHttpErrorToRestError(block: (HttpError<T, E>) -> E?): RestResult<T, E> {
		return when (this) {
			is ConnectionError -> ConnectionError(exception)
			is HttpError -> {
				val replacement = block(this) ?: return HttpError(code)
				return RestError(replacement)
			}

			is ParseError -> ParseError(exception, json)
			is RestError -> RestError(error)
			is Ok -> Ok(data)
		}
	}

	fun unwrap(): T? {
		return (this as? Ok<T, E>)?.data
	}

	fun unwrapRestError(): E? {
		return (this as? RestError<T, E>)?.error
	}

	fun unwrapParseError(): ParseError<T, E>? {
		return this as? ParseError<T, E>
	}

	fun unwrapHttpError(): HttpError<T, E>? {
		return this as? HttpError<T, E>
	}

	fun unwrapConnectionError(): ConnectionError<T, E>? {
		return this as? ConnectionError<T, E>
	}

	fun isOk(): Boolean {
		return this is Ok
	}

	fun isError(): Boolean {
		return this is Error
	}

	fun isRestError(): Boolean {
		return this is RestError
	}

	fun isHttpError(): Boolean {
		return this is HttpError
	}

	fun isParseError(): Boolean {
		return this is ParseError
	}

	fun isConnectionError(): Boolean {
		return this is ConnectionError
	}

	fun descriptionString(): String {
		return when (this) {
			is Ok -> "Ok: ${this.data.toString().ellipsisSingleLine(10)}"
			is ConnectionError -> "Connection error: ${exception.message?.ellipsisSingleLine(30)}"
			is RestError -> "Rest error: ${this.error.toString().ellipsisSingleLine(30)}"
			is HttpError -> "Http error $code"
			is ParseError -> {
				"Parse error: ${exception.message?.ellipsisSingleLine(30)} " + "of json: ${json.ellipsisSingleLine(30)}"
			}
		}
	}
}