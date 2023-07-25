package com.makki.exchanges.abtractions

import com.makki.exchanges.common.Result
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface RestApi {
	val json: Json
		get() = JsonParser.default

	fun Map<String, Any>.toQuery(): String {
		return this.map { "${it.key}=${it.value}" }.joinToString("&")
	}

	fun Int.isOkHttpCode(): Boolean {
		return this in 200..299
	}

	interface ErrorValidator {
		fun isNotDefault(): Boolean
	}
}

inline fun <reified Ok : Any, reified Error : RestApi.ErrorValidator> RestApi.defaultParse(
	response: ClientResponse,
): Result<Ok, RemoteCallError<Error>> {

	val ok = when (response) {
		is ClientResponse.ConnectionError -> return Result.Error(RemoteCallError.ConnectionError(response.e))
		is ClientResponse.Ok -> response
	}

	if (!ok.httpCode.isOkHttpCode()) {
		return Result.Error(RemoteCallError.HttpError(ok.httpCode))
	}
	try {
		val error: Error = json.decodeFromString<Error>(ok.text)

		// decoded, check if it's a fallback of empty fields or not (for example ignoreUnknown + default values)
		if (error.isNotDefault()) {
			return Result.Error(RemoteCallError.ApiError(error))
		}
	} catch (e: SerializationException) {
		// ignore failed parse, do not ignore invalid object
	}

	return try {
		val decode: Ok = json.decodeFromString<Ok>(ok.text)
		Result.Ok(decode)
	} catch (e: SerializationException) {
		Result.Error(RemoteCallError.ParseError(e, ok.text))
	}
}

