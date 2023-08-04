package com.makki.exchanges.models

import com.makki.exchanges.tools.eqIgC
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Market pairs are constructed as traits together as some exchanges do not provide all data
 */
interface MarketPair {
	val base: String
	val quote: String

	fun baseCurrency() = Currency(base)
	fun quoteCurrency() = Currency(quote)

	fun prettyName() = "$base$quote".uppercase() // do not use for exchange

	fun hasCurrency(currency: String) = currency.eqIgC(base) || currency.eqIgC(quote)
	fun hasCurrency(currency: Currency) = currency.name.eqIgC(base) || currency.name.eqIgC(quote)

	fun getOpposite(currency: String): String? {
		if (currency.eqIgC(base)) return quote
		if (currency.eqIgC(quote)) return base
		return null
	}

	fun getOpposite(currency: Currency): Currency? {
		if (currency.name.eqIgC(base)) return quoteCurrency()
		if (currency.name.eqIgC(quote)) return baseCurrency()
		return null
	}

	fun eq(other: MarketPair): Boolean {
		return base.eqIgC(other.base) && quote.eqIgC(other.quote)
	}
}

interface MarketPairPreciseTrait : MarketPair {
	val basePrecision: Int
	val quotePrecision: Int

	fun roundOfBase(value: BigDecimal, mode: RoundingMode): BigDecimal {
		return value.setScale(basePrecision, mode).stripTrailingZeros()
	}

	fun roundOfQuote(value: BigDecimal, mode: RoundingMode): BigDecimal {
		return value.setScale(quotePrecision, mode).stripTrailingZeros()
	}
}

interface MarketPairMinimumTrait : MarketPairPreciseTrait {
	val minBaseVolume: Double
	val minBasePrice: Double
}

interface MarketRatioTrait : MarketPair {
	val makerRatio: Double
	val takeRatio: Double
}

interface MarketPairFull : MarketPair, MarketRatioTrait, MarketPairMinimumTrait, MarketPairPreciseTrait

data class SimpleMarketPair(
	override val base: String,
	override val quote: String,
) : MarketPair


data class DetailedMarketPair(
	override val base: String,
	override val quote: String,
	override val basePrecision: Int,
	override val quotePrecision: Int,
	override val minBaseVolume: Double,
	override val minBasePrice: Double,
	override val makerRatio: Double,
	override val takeRatio: Double,
) : MarketPairFull