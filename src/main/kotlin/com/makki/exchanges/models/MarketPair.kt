package com.makki.exchanges.models

import com.makki.exchanges.tools.eqIgC

/**
 * Market pairs are constructed as traits together as some exchanges do not provide all data
 */
interface MarketPair {
	val base: String
	val quote: String

	fun pairName() = "$base$quote".uppercase()
	fun hasCurrency(currency: String) = currency.eqIgC(base) || currency.eqIgC(quote)
	fun getOpposite(currency: String): String? {
		if (currency.eqIgC(base)) return quote
		if (currency.eqIgC(quote)) return base
		return null
	}
}

interface MarketPairPreciseTrait : MarketPair {
	val basePrecision: Int
	val quotePrecision: Int
}

interface MarketPairMinimumTrait : MarketPairPreciseTrait {
	val minBaseVolume: Double
	val minBasePrice: Double
}

interface MarketRatioTrait : MarketPair {
	val makerRatio: Double
	val takeRatio: Double
}

data class SimpleMarketPair(
	override val base: String,
	override val quote: String,
): MarketPair


data class DetailedMarketPair(
	override val base: String,
	override val quote: String,
	override val basePrecision: Int,
	override val quotePrecision: Int,
	override val minBaseVolume: Double,
	override val minBasePrice: Double,
	override val makerRatio: Double,
	override val takeRatio: Double,
) : MarketPairMinimumTrait, MarketRatioTrait