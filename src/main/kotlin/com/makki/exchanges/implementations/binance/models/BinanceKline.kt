package com.makki.exchanges.implementations.binance.models

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.*

@Serializable(with = BinanceKlineSerializer::class)
data class BinanceKline(
	val start: Long,
	val end: Long,
	val open: Double,
	val high: Double,
	val low: Double,
	val close: Double,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BinanceKline::class)
object BinanceKlineSerializer : KSerializer<BinanceKline> {
	override fun deserialize(decoder: Decoder): BinanceKline {
		val element = (decoder as JsonDecoder).decodeJsonElement()
		require(element is JsonArray)

		return BinanceKline(
			start = element[0].jsonPrimitive.long,
			end = element[6].jsonPrimitive.long,
			open = element[1].jsonPrimitive.double,
			high = element[2].jsonPrimitive.double,
			low = element[3].jsonPrimitive.double,
			close = element[4].jsonPrimitive.double,
		)
	}
}

@Serializable
data class BinanceSocketKlineMsg(
	val e: String,  // event
	val s: String,  // symbol
	val k: BinanceSocketKlineAsset,
)

@Serializable
data class BinanceSocketKlineAsset(
	@SerialName("t")
	val start: Long,
	@SerialName("T")
	val end: Long,
	@SerialName("o")
	val open: Double,
	@SerialName("c")
	val close: Double,
	@SerialName("h")
	val high: Double,
	@SerialName("l")
	val low: Double,
	@SerialName("v")
	val volume: Double,
	@SerialName("n")
	val trades: Int,
)