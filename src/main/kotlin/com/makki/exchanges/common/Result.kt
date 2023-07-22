package com.makki.exchanges.common

/**
 * This is a generic sealed class for functional programming on single object instances
 * Inspired by RUST <3
 */
sealed interface Result<T, E> {
	data class Ok<T, E>(val data: T) : Result<T, E>
	data class Error<T, E>(val error: E) : Result<T, E>

	fun isOk(): Boolean {
		return this is Ok
	}

	fun isError(): Boolean {
		return this is Error
	}

	fun unwrapOk(): T? {
		return (this as? Ok<T, E>)?.data
	}

	fun unwrapError(): E? {
		return (this as? Error<T, E>)?.error
	}

	@Throws(IllegalStateException::class)
	fun requireError(): E {
		return (this as? Error<T, E>)?.error ?: throw IllegalStateException("Not an error")
	}

	fun unwrapAny() = unwrapOk() ?: unwrapError()

	fun unwrapOrThrow(block: (E) -> Exception): T {
		when (this) {
			is Error -> throw block(this.error)
			is Ok -> return this.data
		}
	}

	@Throws(IllegalStateException::class)
	fun unwrapOrThrow(): T {
		return unwrapOrThrow {
			throw IllegalStateException("Unwrap failed")
		}
	}
}


/*
 * Extensions
 */
// region extension

inline fun <T, E> Result<T, E>.onOk(block: (T) -> Unit): Result<T, E> {
	block(this.unwrapOk() ?: return this)
	return this
}

inline fun <T, E> Result<T, E>.onError(block: (E) -> Unit): Result<T, E> {
	block(this.unwrapError() ?: return this)
	return this
}

inline fun <T, E> Result<T, E>.unwrapOrFallback(block: () -> T): T {
	return unwrapOk() ?: block()
}

fun <T, E> Result<T, E>.unwrapOrFallback(value: T): T {
	return unwrapOk() ?: value
}

inline fun <T, E, N> Result<T, E>.mapOk(block: (T) -> N): Result<N, E> {
	return when (this) {
		is Result.Error -> Result.Error(this.error)
		is Result.Ok -> Result.Ok(block(this.data))
	}
}

inline fun <T, E, N> Result<T, E>.mapError(block: (E) -> N): Result<T, N> {
	return when (this) {
		is Result.Error -> Result.Error(block(error))
		is Result.Ok -> Result.Ok(data)
	}
}

inline fun <C, T, E> Result<T, E>.flatMapResult(block: (T) -> Result<C, E>): Result<C, E> {
	return when (this) {
		is Result.Error -> Result.Error(this.error)
		is Result.Ok -> {
			when (val inner = block(this.data)) {
				is Result.Error -> Result.Error(inner.error)
				is Result.Ok -> Result.Ok(inner.data)
			}
		}
	}
}

/*
 * Specific subtypes
 */

// same type error flatten
fun <T, E> Result<Result<T, E>, E>.flatten(): Result<T, E> {
	return when (this) {
		is Result.Error -> Result.Error(this.error)
		is Result.Ok -> {
			when (val inner = this.data) {
				is Result.Error -> Result.Error(inner.error)
				is Result.Ok -> Result.Ok(inner.data)
			}
		}
	}
}

// different type errors to one
inline fun <T, E, M> Result<Result<T, E>, M>.flattenError(block: (M) -> E): Result<T, E> {
	return when (this) {
		is Result.Error -> Result.Error(block(this.error))
		is Result.Ok -> {
			when (val inner = this.data) {
				is Result.Error -> Result.Error(inner.error)
				is Result.Ok -> Result.Ok(inner.data)
			}
		}
	}
}

// different type errors to one
inline fun <T, E, M> Result<Result<T, E>, M>.flattenErrorRev(block: (E) -> M): Result<T, M> {
	return when (this) {
		is Result.Error -> Result.Error(this.error)
		is Result.Ok -> {
			when (val inner = this.data) {
				is Result.Error -> Result.Error(block(inner.error))
				is Result.Ok -> Result.Ok(inner.data)
			}
		}
	}
}

// separation of
fun <T, E> List<Result<T, E>>.unwrapCollect(): Pair<List<T>, List<E>> {
	val oks = this.mapNotNull { it as? Result.Ok }.map { it.data }
	val errs = this.mapNotNull { it as? Result.Error }.map { it.error }
	return Pair(oks, errs)
}

fun <C> Result<C, C>.toCommon(): C {
	return when (this) {
		is Result.Error -> this.error
		is Result.Ok -> this.data
	}
}

fun <C, T : C, E : C> Result<T, E>.toLowerCommon(): C {
	return when (this) {
		is Result.Error -> this.error
		is Result.Ok -> this.data
	}
}

fun <T, C, I : C, E : C> Result<Result<T, I>, E>.flattenToCommon(): Result<T, C> {
	return when (this) {
		is Result.Error -> Result.Error(this.error)
		is Result.Ok -> when (val inner = this.data) {
			is Result.Error -> Result.Error(inner.error)
			is Result.Ok -> Result.Ok(inner.data)
		}
	}
}

/*
 * Exceptions
 */

fun <N, T> Result<T, Exception>.mapAndCatch(block: (T) -> N): Result<N, Exception> {
	return this.flatMapResult {
		try {
			return@flatMapResult Result.Ok(block(it))
		} catch (e: Exception) {
			return@flatMapResult Result.Error(e)
		}
	}
}

fun <T, E : Exception> Result<T, E>.unwrapOrThrowInner(): T {
	when (this) {
		is Result.Error -> throw this.error
		is Result.Ok -> return this.data
	}
}

fun <T, E : Exception> Result<T, E>.throwInnerIfError(): Result<T, E>? {
	if (this is Result.Error) {
		throw this.error
	}
	return null
}

fun <R, T, E> Result<T, E>.returnError(): Result<R, E> {
	return this.requireError().wrapError()
}

fun <T, E> E.wrapError(): Result<T, E> {
	return Result.Error(this)
}

fun <T, E> T.wrapOk(): Result<T, E> {
	return Result.Ok(this)
}

// endregion