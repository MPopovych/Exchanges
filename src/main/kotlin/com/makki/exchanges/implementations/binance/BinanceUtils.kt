package com.makki.exchanges.implementations.binance

import com.makki.exchanges.abtractions.ExchangeUtil

object BinanceUtils : ExchangeUtil {

	override fun getApiMarketName(base: String, quote: String): String {
		return "${base.lowercase()}${quote.lowercase()}"
	}

}