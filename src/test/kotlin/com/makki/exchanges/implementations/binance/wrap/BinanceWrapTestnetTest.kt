package com.makki.exchanges.implementations.binance.wrap

import com.makki.exchanges.SupportedExchanges
import com.makki.exchanges.models.MarketPairPreciseTrait
import com.makki.exchanges.models.MarketRatioTrait
import com.makki.exchanges.models.OrderState
import com.makki.exchanges.nontesting.TestLogger
import com.makki.exchanges.nontesting.asyncTest
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

class BinanceWrapTestnetTest {

	companion object {
		// these are for a testnet spot, be nice and don't use them :)
		private const val GIT_TESTNET_API_KEY = "2hrIQazhFkpUdL8a9WvEN9ng5L8X9VAqYtrLpIxqX0o0seBQrnR5AJ2PIAX3bEsc"
		private const val GIT_TESTNET_SECRET_KEY = "hUnNsRKumXARGqGhdXTVB4iMl8Rf99jEEjnd5rU41TWE3yNUwchhQChP1BzOklcy"

		private val exchange = SupportedExchanges.BinanceTestNet(GIT_TESTNET_API_KEY, GIT_TESTNET_SECRET_KEY)
	}

	@Test
	fun testBinanceMarketInfoRequest() = asyncTest {
		val response = exchange.wrap.marketInfo()
		assert(response.isOk()) { response }

		val marketInfo = response.unwrapOk()
		assert(!marketInfo.isNullOrEmpty())
		println(marketInfo?.take(10))
	}

	@Test
	fun testBinanceKlineRequest() = asyncTest {
		val response = exchange.wrap.klineData("BTCUSDT", "15m", limit = 10)
		assert(response.isOk()) { response }

		val klineList = response.unwrapOk()
		assert(!klineList.isNullOrEmpty())
		println(klineList?.takeLast(5))
	}

	@Test
	fun testBinanceComposeOrder() = asyncTest {
		val pairs = exchange.wrap.marketInfo().also { response ->
			assert(response.isOk()) { response }
		}.unwrapOrThrow()

		val btcPair = pairs.find { it.prettyName() == "BTCUSDT" }
				as? MarketPairPreciseTrait ?: throw IllegalStateException()

		TestLogger.logger.printInfoPositive("Found ${btcPair.prettyName()}")
		delay(500)

		val interval = exchange.intervals.find { it.prettyName == "15m" } ?: throw IllegalStateException()
		val kline = exchange.wrap.klineData(btcPair, interval).also { response ->
			assert(response.isOk()) { response }
		}.unwrapOrThrow()

		val targetPrice = btcPair.roundOfQuote(BigDecimal.valueOf(kline.last().high) * BigDecimal.valueOf(1.1), RoundingMode.HALF_UP)
		val spendVolume = BigDecimal.valueOf(0.001)
		val gainVolume = spendVolume * targetPrice

		TestLogger.logger.printInfoPositive(
			"Found ${interval.prettyName} klines, target price: ${targetPrice}, " +
					"spend: ${spendVolume}, gain: $gainVolume"
		)

		val openOrder = exchange.wrap.createLimitOrder(
			pair = btcPair,
			spend = btcPair.baseCurrency(),
			spendVolume = spendVolume,
			gainVolume = gainVolume,
			price = targetPrice
		).also { response ->
			assert(response.isOk()) { response }
		}.unwrapOrThrow()

		TestLogger.logger.printInfoPositive("Opened order $openOrder")

		delay(2000)

		val cancelOrder = exchange.wrap.cancelOrder(openOrder).also { response ->
			assert(response.isOk()) { response }
		}.unwrapOrThrow()

		TestLogger.logger.printInfoPositive("Closed order $cancelOrder")

		assert(openOrder.isOpen())
		assert(!openOrder.isClosed())
		assert(!openOrder.isFilled())
		assetDecimalEq(targetPrice, openOrder.price)
		assetDecimalEq(spendVolume, openOrder.spendOrigVolume)
		assetDecimalEq(gainVolume, openOrder.gainOrigVolume)
		assetDecimalEq(BigDecimal.ZERO, openOrder.spendFilledVolume)
		assetDecimalEq(BigDecimal.ZERO, openOrder.gainFilledVolume)
		assertEquals(btcPair, openOrder.pair)
		assertEquals(OrderState.OPEN, openOrder.state)

		assert(!cancelOrder.isOpen())
		assert(cancelOrder.isClosed())
		assert(!cancelOrder.isFilled())
		assetDecimalEq(targetPrice, cancelOrder.price)
		assetDecimalEq(spendVolume, cancelOrder.spendOrigVolume)
		assetDecimalEq(gainVolume, cancelOrder.gainOrigVolume)
		assetDecimalEq(BigDecimal.ZERO, cancelOrder.spendFilledVolume)
		assetDecimalEq(BigDecimal.ZERO, cancelOrder.gainFilledVolume)
		assertEquals(btcPair, cancelOrder.pair)
		assertEquals(OrderState.CLOSED, cancelOrder.state)
	}

	private fun assetDecimalEq(a: BigDecimal, b: BigDecimal) {
		assertEquals(a.stripTrailingZeros(), b.stripTrailingZeros())
	}

}