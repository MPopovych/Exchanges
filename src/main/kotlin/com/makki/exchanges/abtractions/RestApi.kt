package com.makki.exchanges.abtractions

import kotlinx.serialization.json.Json

interface RestApi {
	val json: Json
		get() = JsonParser.default

	fun Map<String, Any>.toQuery(): String {
		return this.map { "${it.key}=${it.value}" }.joinToString("&")
	}

	fun Int.isOkHttpCode(): Boolean {
		return this in (200..<300)
	}

	interface ErrorValidator {
		fun isNotDefault(): Boolean
	}
}

inline fun <reified Ok : Any, reified Error : RestApi.ErrorValidator> RestApi.defaultParse(response: ClientResponse): RestResult<Ok, Error> {
	val ok = when (response) {
		is ClientResponse.Error -> return RestResult.ConnectionError(response.e)
		is ClientResponse.Ok -> response
	}

	if (!ok.httpCode.isOkHttpCode()) {
		return RestResult.HttpError(ok.httpCode)
	}
	try {
		val error: Error = json.decodeFromString<Error>(ok.text)
		if (error.isNotDefault()) {
			return RestResult.RestError(error)
		}
	} catch (_: Exception) {
	}

	return try {
		val decode: Ok = json.decodeFromString<Ok>(ok.text)
		RestResult.Ok(decode)
	} catch (e: Exception) {
		RestResult.ParseError(e, ok.text)
	}
}

