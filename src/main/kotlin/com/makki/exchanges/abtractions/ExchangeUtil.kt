package com.makki.exchanges.abtractions

import com.makki.exchanges.models.MarketPair

/**
 * Interface for utility object of namings, market codes, enumerations, etc
 */
interface ExchangeUtil {
	fun getApiMarketName(base: String, quote: String): String
	fun getApiMarketName(marketPair: MarketPair): String {
		return getApiMarketName(marketPair.base, marketPair.quote)
	}
}