@file:UseSerializers(BigDecimalSerializer::class)

package com.makki.exchanges.implementations.binance.models

import com.makki.exchanges.common.serializers.BigDecimalSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.math.BigDecimal

@Serializable
data class BinanceUserData(
	val makerCommission: Int,
	val takerCommission: Int,
	val commissionRates: Map<String, BigDecimal>, // serializer is declared in file annotation
	val canTrade: Boolean,
	val canWithdraw: Boolean,
	val canDeposit: Boolean,
	val accountType: String,
	val balances: List<BinanceBalanceEntry>,
	val permissions: List<String>,
	val uid: Long,
)