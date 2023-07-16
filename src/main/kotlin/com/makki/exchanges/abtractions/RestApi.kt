package com.makki.exchanges.abtractions

import com.makki.exchanges.implementations.BasicResponse
import kotlinx.serialization.json.Json

interface RestApi {

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

inline fun <reified Ok, reified Error : RestApi.ErrorValidator> RestApi.defaultParse(response: BasicResponse): RestResult<Ok, Error> {
	val ok = when (response) {
		is BasicResponse.Error -> return RestResult.ConnectionError(response.e)
		is BasicResponse.Ok -> response
	}

	if (!ok.httpCode.isOkHttpCode()) {
		return RestResult.HttpError(ok.httpCode)
	}
	try {
		val error: Error = Json.decodeFromString<Error>(ok.text)
		if (error.isNotDefault()) {
			return RestResult.RestError(error)
		}
	} catch (_: Exception) {
	}

	return try {
		val decode: Ok = Json.decodeFromString<Ok>(ok.text)
		RestResult.Ok(decode)
	} catch (e: Exception) {
		RestResult.ParseError(e, ok.text)
	}
}

