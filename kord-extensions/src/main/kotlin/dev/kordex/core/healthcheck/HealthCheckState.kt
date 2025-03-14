/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.healthcheck

import dev.kordex.core.healthcheck.serializers.HealthCheckStateSerializer
import kotlinx.serialization.Serializable

@Serializable(with = HealthCheckStateSerializer::class)
public sealed class HealthCheckState(
	public val type: String,
	public val ordinal: Int
) {
	public companion object {
		public const val UNHEALTHY_ORDINAL: Int = 0
		public const val STARTING_ORDINAL: Int = 50
		public const val HEALTHY_ORDINAL: Int = 100

		public fun addState(type: String, ordinal: Int): HealthCheckState =
			Other(type, ordinal).also { Registry.stateMap[type] = it }

		public fun getState(type: String): HealthCheckState? =
			Registry.stateMap[type]

		public fun removeState(type: String): Boolean =
			Registry.stateMap.remove(type) != null
	}

	public object Registry {
		public val stateMap: MutableMap<String, HealthCheckState> = mutableMapOf(
			Unhealthy.type to Unhealthy,
			Starting.type to Starting,
			Healthy.type to Healthy,
		)
	}

	public object Unhealthy : HealthCheckState("unhealthy", UNHEALTHY_ORDINAL)
	public object Starting : HealthCheckState("starting", STARTING_ORDINAL)
	public object Healthy : HealthCheckState("healthy", HEALTHY_ORDINAL)

	internal class Other(type: String, ordinal: Int) : HealthCheckState(type, ordinal)
}
