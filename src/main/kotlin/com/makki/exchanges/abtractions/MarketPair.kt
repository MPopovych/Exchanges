package com.makki.exchanges.abtractions

interface MarketPair {
	val base: String
	val quote: String

	val basePrecision: Int?
	val quotePrecision: Int?
}

interface MarketPairPrecise {
	val basePrecision: Int
	val quotePrecision: Int
}

interface MarketPairMinimum {
	val basePrecision: Int
	val quotePrecision: Int
}