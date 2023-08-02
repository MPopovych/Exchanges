package com.makki.exchanges.abtractions

import com.makki.exchanges.common.Result
import com.makki.exchanges.tools.urlEncodeUTF8
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface RestApi {
	val json: Json
		get() = JsonParser.default

	fun Map<String, Any>.toQueryEncodedUTF8(): String {
		return this.map { "${it.key.urlEncodeUTF8()}=${it.value.toString().urlEncodeUTF8()}" }.joinToString("&")
	}

	fun Int.isOkHttpCode(): Boolean {
		return this in 200..299
	}

	interface ErrorValidator {
		fun isNotDefault(): Boolean
		fun isError(): Boolean = isNotDefault()
	}
}

/**
 * Default processing with such priorities:
 *  Connection error > api specific error > http error code > parse error
 */
inline fun <reified Ok : Any, reified Error : RestApi.ErrorValidator> RestApi.defaultParse(
	response: ClientResponse,
): Result<Ok, RemoteCallError<Error>> {

	// first - check for connection error
	val ok = when (response) {
		is ClientResponse.ConnectionError -> return Result.Error(RemoteCallError.ConnectionError(response.e))
		is ClientResponse.Ok -> response
	}

	// second - try the exchange specific description
	try {
		val error: Error = json.decodeFromString<Error>(ok.text)

		// decoded, check if it's a fallback of empty fields or not (for example ignoreUnknown + default values)
		if (error.isError() || error.isNotDefault()) {
			return Result.Error(RemoteCallError.ApiError(error))
		}
	} catch (e: SerializationException) {
		// ignore failed parse, do not ignore invalid object
	}
	// third - check for http error
	if (!ok.httpCode.isOkHttpCode()) {
		return Result.Error(RemoteCallError.HttpError(ok.httpCode, ok.text))
	}
	// fourth - try to parse
	return try {
		val decode: Ok = json.decodeFromString<Ok>(ok.text)
		Result.Ok(decode)
	} catch (e: SerializationException) {
		Result.Error(RemoteCallError.ParseError(e, ok.text))
	}
}

