package com.makki.exchanges.abtractions

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

	@Suppress("UNCHECKED_CAST")
	fun <F : Any> unwrap(): F? {
		return (this as? Ok<F, Any>)?.data
	}

	@Suppress("UNCHECKED_CAST")
	fun <E : Any> unwrapRestError(): E? {
		return (this as? RestError<Any, E>)?.error
	}

	@Suppress("UNCHECKED_CAST")
	fun unwrapParseError(): ParseError<Any, Any>? {
		return this as? ParseError<Any, Any>
	}

	@Suppress("UNCHECKED_CAST")
	fun unwrapHttpError(): HttpError<Any, Any>? {
		return this as? HttpError<Any, Any>
	}

	@Suppress("UNCHECKED_CAST")
	fun unwrapConnectionError(): ConnectionError<Any, Any>? {
		return this as? ConnectionError<Any, Any>
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
}