package com.makki.exchanges.common.serializers

import com.makki.exchanges.tools.trimStr
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

object BigDecimalSerializer : KSerializer<BigDecimal> {
	override fun deserialize(decoder: Decoder): BigDecimal {
		return decoder.decodeString().toBigDecimal()
	}

	override fun serialize(encoder: Encoder, value: BigDecimal) {
		encoder.encodeString(value.trimStr())
	}

	override val descriptor: SerialDescriptor
		get() = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)
}