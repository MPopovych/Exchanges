package com.makki.exchanges.implementations.binance.models

import com.makki.exchanges.models.Kline
import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.*

@Serializable(with = BinanceKlineSerializer::class)
data class BinanceKline(
	override val start: Long,
	override val end: Long,
	override val open: Double,
	override val high: Double,
	override val low: Double,
	override val close: Double,
	override val volume: Double,
	override val trades: Int,
): Kline

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
			volume = element[5].jsonPrimitive.double,
			trades = element[8].jsonPrimitive.int
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
	@SerialName("s")
	val market: String,
	@SerialName("t")
	override val start: Long,
	@SerialName("T")
	override val end: Long,
	@SerialName("o")
	override val open: Double,
	@SerialName("c")
	override val close: Double,
	@SerialName("h")
	override val high: Double,
	@SerialName("l")
	override val low: Double,
	@SerialName("v")
	override val volume: Double,
	@SerialName("n")
	override val trades: Int,
): Kline