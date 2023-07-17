package com.makki.exchanges.implementations.binance.models

import com.makki.exchanges.abtractions.KlineInterval
import java.util.concurrent.TimeUnit

enum class BinanceKlineInterval(
	private val code: String,
	private val time: Long,
) : KlineInterval {

	Minutes3("3m", TimeUnit.MINUTES.toMillis(3)),
	Minutes5("5m", TimeUnit.MINUTES.toMillis(5)),
	Minutes15("15m", TimeUnit.MINUTES.toMillis(15)),
	Minutes30("30m", TimeUnit.MINUTES.toMillis(30)),
	Hour1("1h", TimeUnit.HOURS.toMillis(1)),
	;

	override val apiCode: String
		get() = code
	override val prettyName: String
		get() = code
	override val msValue: Long
		get() = time

}