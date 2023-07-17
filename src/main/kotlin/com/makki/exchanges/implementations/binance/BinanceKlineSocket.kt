package com.makki.exchanges.implementations.binance

import com.makki.exchanges.abtractions.JsonParser
import com.makki.exchanges.abtractions.KlineInterval
import com.makki.exchanges.implementations.SelfManagingSocket
import com.makki.exchanges.implementations.binance.models.BinanceSocketKlineAsset
import com.makki.exchanges.implementations.binance.models.BinanceSocketKlineMsg
import com.makki.exchanges.logging.printLog
import com.makki.exchanges.tools.CachedSubject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.CopyOnWriteArraySet

class BinanceKlineSocket(private val intervalString: String) {

	constructor(interval: KlineInterval): this(interval.apiCode)

	private val marketList = CopyOnWriteArraySet<String>()
	private val json = JsonParser.default

	private val socket = SelfManagingSocket.builder("BinanceKlineSocket")
		.url("wss://stream.binance.com:9443/ws/stream")
		.onConnectionOpen {
			this.send(getSubscriptionMessages())
		}
		.onTextMsg { parse(it) }
		.build()

	private val subject = CachedSubject<BinanceSocketKlineAsset>(overflow = BufferOverflow.DROP_OLDEST)

	fun observe() = subject.asSharedFlow()

	fun start() {
		socket.start()
	}

	fun stop() {
		socket.close()
	}

	fun lazyStop() {
		socket.stopOnNext()
	}

	/**
	 * runtime adding of market
	 */
	suspend fun addMarket(market: String) {
		if (market in marketList) return

		marketList.add(market)
		val subMsg = wrapStreams(marketToRequest(market))
		socket.send(subMsg)
	}

	private suspend fun parse(msg: String) {
		val kline: BinanceSocketKlineMsg = try {
			json.decodeFromString<BinanceSocketKlineMsg>(msg)
		} catch (e: Exception) {
			if (msg.contains("code") || msg.contains("msg")) {
				printLog("Error in socket, response: $msg")
			}
			return
		}
		subject.emit(kline.k)
	}

	private fun marketToRequest(market: String): String {
		return "\"${market.lowercase()}@kline_${intervalString}\""
	}

	private fun getSubscriptionMessages(): String {
		val streams = marketList.joinToString(",") { marketToRequest(it) }
		return wrapStreams(streams)
	}

	private fun wrapStreams(streams: String): String {
		return """
{
  "method": "SUBSCRIBE",
  "params": [
    $streams
  ],
  "id": 1
}
"""
	}

}