package com.makki.exchanges.abtractions


/**
 * @param E - can be an expected api exception class
 */
sealed interface RemoteCallError<E> {
	data class ApiError<E>(val error: E) : RemoteCallError<E>
	data class HttpError<E>(val code: Int) : RemoteCallError<E>
	data class ParseError<E>(val exception: Exception, val response: String) : RemoteCallError<E>
	data class ConnectionError<E>(val exception: Exception) : RemoteCallError<E>
}

fun RemoteCallError.HttpError<*>.toEnum(): HttpCodeDescription = HttpCodeDescription.findError(code)

sealed interface RestResult<T, E> {
	data class Ok<T, E>(val data: T) : RestResult<T, E>

	sealed interface Error<T, E> : RestResult<T, E>
	data class RestError<T, E>(val error: E) : Error<T, E>
	data class HttpError<T, E>(val code: Int) : Error<T, E> {
		fun toEnum(): HttpCodeDescription = HttpCodeDescription.findError(code)
	}

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

	fun tryMapHttpToRestError(block: (HttpError<T, E>) -> E?): RestResult<T, E> {
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

}