/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.healthcheck.serializers

import dev.kordex.core.healthcheck.HealthCheckState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public class HealthCheckStateSerializer : KSerializer<HealthCheckState> {
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HealthCheckState", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: HealthCheckState) {
		encoder.encodeString(value.type)
	}

	override fun deserialize(decoder: Decoder): HealthCheckState {
		val type = decoder.decodeString()

		return HealthCheckState.getState(type)
			?: error("Unknown health-check state: $type")
	}
}
