package com.makki.exchanges.implementations.binance.models

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class BinanceMarketPair(
	val status: String,
	@SerialName("baseAsset")
	val base: String,
	@SerialName("quoteAsset")
	val quote: String,
	@SerialName("baseAssetPrecision")
	val basePrecision: Int,
	@SerialName("quoteAssetPrecision")
	val quotePrecision: Int,
	@SerialName("isMarginTradingAllowed")
	val marginAllowed: Boolean,
	@SerialName("isSpotTradingAllowed")
	val spotAllowed: Boolean,
	val orderTypes: List<String>,
	val filters: List<BinanceMarketPairFilter>,
)

@Serializable(with = BinanceFilterSerializer::class)
sealed class BinanceMarketPairFilter {
	@Serializable
	data class PriceFilter(
		val minPrice: Double,
		val maxPrice: Double,
		val tickSize: String,
	) : BinanceMarketPairFilter()

	@Serializable
	data class LotSizeFilter(
		val minQty: Double,
		val maxQty: Double,
		val stepSize: String,
	) : BinanceMarketPairFilter()

	@Serializable
	data class NotionalFilter(
		val minNotional: Double,
		val maxNotional: Double,
		val avgPriceMins: Int,
	) : BinanceMarketPairFilter()

	@Serializable
	object Unknown : BinanceMarketPairFilter()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = BinanceMarketPairFilter::class)
object BinanceFilterSerializer :
	JsonContentPolymorphicSerializer<BinanceMarketPairFilter>(BinanceMarketPairFilter::class) {
	override fun selectDeserializer(
		element: JsonElement,
	): DeserializationStrategy<BinanceMarketPairFilter> {

		return when (element.jsonObject["filterType"]?.jsonPrimitive?.contentOrNull) {
			"PRICE_FILTER" -> BinanceMarketPairFilter.PriceFilter.serializer()
			"LOT_SIZE" -> BinanceMarketPairFilter.LotSizeFilter.serializer()
			"NOTIONAL" -> BinanceMarketPairFilter.NotionalFilter.serializer()
			else -> BinanceMarketPairFilter.Unknown.serializer()
		}
	}
}

@Serializable
class BinanceMarketInfo(
	val symbols: List<BinanceMarketPair>,
)