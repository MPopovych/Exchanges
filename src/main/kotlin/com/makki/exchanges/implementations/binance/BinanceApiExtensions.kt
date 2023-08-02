package com.makki.exchanges.implementations.binance

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private object BinanceConst {
	const val HMAC_ALGO = "HmacSHA256"
}

fun BinanceApi.toHmacSha256(data: String, salt: String): ByteArray {
	val digest = Mac.getInstance(BinanceConst.HMAC_ALGO)
	val signingKey = SecretKeySpec(salt.toByteArray(Charsets.UTF_8), BinanceConst.HMAC_ALGO)
	digest.init(signingKey)
	return digest.doFinal(data.toByteArray(Charsets.UTF_8))
}

fun BinanceApi.toHex(byteArray: ByteArray) = byteArray.joinToString(separator = "") { byte -> "%02x".format(byte) }