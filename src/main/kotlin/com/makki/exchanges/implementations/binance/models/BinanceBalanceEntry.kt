@file:UseSerializers(BigDecimalSerializer::class)

package com.makki.exchanges.implementations.binance.models

import com.makki.exchanges.common.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigDecimal

@Serializable
data class BinanceBalanceEntry(
	val asset: String,
	val free: BigDecimal,
	val locked: BigDecimal,
)