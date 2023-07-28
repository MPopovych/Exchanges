package com.makki.exchanges.implementations.binance

import com.makki.exchanges.abtractions.Frame
import com.makki.exchanges.abtractions.JsonParser
import com.makki.exchanges.abtractions.KlineInterval
import com.makki.exchanges.abtractions.StateObservable
import com.makki.exchanges.implementations.SelfManagingSocket
import com.makki.exchanges.implementations.binance.models.BinanceSocketKlineAsset
import com.makki.exchanges.implementations.binance.models.BinanceSocketKlineMsg
import com.makki.exchanges.logging.LogLevel
import com.makki.exchanges.logging.loggerBuilder
import com.makki.exchanges.tools.CachedStateSubject
import com.makki.exchanges.tools.StateObserver
import com.makki.exchanges.tools.ellipsis
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.CopyOnWriteArraySet

class BinanceKlineSocket(socket: SelfManagingSocket? = null) : StateObservable {

	private val logger = loggerBuilder().level(LogLevel.Info).build()
	private val marketList = CopyOnWriteArraySet<Subscription>()
	private val json = JsonParser.default

	private val socket = socket ?: (SelfManagingSocket
		.builder("BinanceKline")
		.url("wss://stream.binance.com:9443/ws/stream")
		.onConnectionOpen {
			subject.emit(Frame.Connect())
			val sent = if (marketList.isNotEmpty()) {
				this.send(getSubscriptionMessages().also {
					logger.printDebug { "sending sub: ${it.replace("\n", " ")}" }
				})
			} else {
				false
			}
			if (!sent) {
				logger.printWarning("failed to send sub msg on open")
			}
		}
		.onTextMsg { parse(it) }
		.onConnectionClosed {
			subject.emit(Frame.Disconnect())
		}
		.onConnectionRejected {
			subject.emit(Frame.Disconnect())
		}
		.build())

	private val subject = CachedStateSubject<Frame<BinanceSocketKlineAsset>>(overflow = BufferOverflow.DROP_OLDEST)

	fun observe() = subject.asSharedFlow()
	fun isActivated() = socket.isActivated()
	fun isRunning() = socket.isRunning()
	fun start() = socket.start()
	fun stop() = socket.close()
	fun lazyStop() = socket.stopOnNext()

	override fun state() = StateObserver()
		.track("active") { isActivated() }
		.track("running") { isRunning() }
		.track("market_count") { marketList.size }

	/**
	 * runtime adding of market
	 */
	suspend fun addMarket(market: String, interval: String) {
		val subscription = Subscription.of(market = market, interval = interval)
		if (subscription in marketList) return

		marketList.add(subscription)
		val subMsg = wrapAddStreams(subscription.toStream())
		logger.printDebug { "sending sub: ${subMsg.replace("\n", " ")}" }
		if (!socket.send(subMsg)) {
			logger.printWarning("failed to send sub msg")
		}
	}

	/**
	 * This should be used carefully, might remove a shared subscription
	 */
	suspend fun removeMarket(market: String, interval: String) {
		val subscription = Subscription.of(market = market, interval = interval)
		if (subscription !in marketList) return

		marketList.remove(subscription)
		val unsubMsg = wrapRemoveStreams(subscription.toStream())
		logger.printDebug { "sending unsub: ${unsubMsg.replace("\n", " ")}" }
		if (!socket.send(unsubMsg)) {
			logger.printWarning("failed to send unsub msg")
		}
	}

	suspend fun addMarket(market: String, interval: KlineInterval) = addMarket(market, interval.apiCode)

	private suspend fun parse(msg: String) {
		logger.printDebug("msg: ${msg.replace("\n", "").ellipsis(130)}")
		val kline: BinanceSocketKlineMsg = try {
			json.decodeFromString<BinanceSocketKlineMsg>(msg)
		} catch (e: Exception) {
			if (msg.contains("code") || msg.contains("msg")) {
				logger.printError("Error in socket, response: $msg")
			}
			return
		}
		subject.emit(Frame.Asset(kline.k))
	}

	private fun getSubscriptionMessages(): String {
		val streams = marketList.joinToString(",") { s -> s.toStream() }
		return wrapAddStreams(streams)
	}

	private fun wrapAddStreams(streams: String): String {
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

	private fun wrapRemoveStreams(streams: String): String {
		return """
{
  "method": "UNSUBSCRIBE",
  "params": [
    $streams
  ],
  "id": 1
}
"""
	}

	@Suppress("DataClassPrivateConstructor")
	private data class Subscription private constructor(val market: String, val interval: String) {
		companion object {
			fun of(market: String, interval: String): Subscription {
				return Subscription(market.lowercase(), interval.lowercase())
			}
		}

		fun toStream(): String {
			return "\"${market}@kline_${interval}\""
		}
	}

}