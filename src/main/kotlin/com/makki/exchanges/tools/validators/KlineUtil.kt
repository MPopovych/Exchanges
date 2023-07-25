package com.makki.exchanges.tools.validators

import com.makki.exchanges.logging.defaultLogger
import com.makki.exchanges.models.Kline
import com.makki.exchanges.models.KlineAsset

object KlineUtil {

	private val logger = defaultLogger()

	/**
	 * Input data needs to be pre sorted
	 */
	fun testIntegrity(kline: List<Kline>, print: Boolean = false): Boolean {
		if (kline.isEmpty()) return false

		var end = kline.first().end
		kline.drop(1).forEach {
			if (end + 1 != it.start) {
				if (print) {
					logger.printWarning("gap of ${it.start - end} in integrity validation at: $end - ${it.end}")
				}
				return false
			}
			end = it.end
		}
		return true
	}

	/**
	 * Sorts, checks for gaps or empty
	 */
	fun processAndClean(kline: List<KlineAsset>): KlineProcessResult {
		if (kline.isEmpty()) return KlineProcessResult.Empty

		val sorted = kline.sortedBy { it.start }
		var known: KlineAsset? = null

		val gaps = ArrayList<Pair<Long, Long>>()

		val cleaned = sorted.mapNotNull {
			val localKnown = known
			if (localKnown != null) {
				if (it.start == localKnown.start) {
					// copy in list, may occur with multiple requests, skip over it
					return@mapNotNull null
				}
				if (localKnown.end + 1 != it.start) {
					// a gap between klines
					gaps.add(Pair(localKnown.end, it.start))
				}
				known = it
				return@mapNotNull it
			} else {
				known = it
				return@mapNotNull it
			}
		}
		if (gaps.isNotEmpty()) {
			return KlineProcessResult.Gap(gaps)
		}
		return KlineProcessResult.Ok(cleaned)
	}

	sealed interface KlineProcessResult {
		class Ok(val data: List<KlineAsset>) : KlineProcessResult
		class Gap(val list: List<Pair<Long, Long>>) : KlineProcessResult
		object Empty : KlineProcessResult
	}

}