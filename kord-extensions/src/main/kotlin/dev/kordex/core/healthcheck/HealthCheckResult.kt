/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.healthcheck

public class HealthCheckResult(
	public val lastState: HealthCheckState,
	public val lastMessage: String?,
) {
	public var state: HealthCheckState = HealthCheckState.Healthy
	public var message: String? = null

	public fun state(state: HealthCheckState, message: String? = null) {
		this.state = state
		this.message = message
	}

	public suspend fun healthyIf(message: String? = null, body: suspend () -> Boolean) {
		if (this.state != HealthCheckState.Healthy && body()) {
			this.state = HealthCheckState.Healthy
			this.message = message
		}
	}

	public suspend fun healthyIfNot(message: String? = null, body: suspend () -> Boolean) {
		if (this.state != HealthCheckState.Healthy && !body()) {
			this.state = HealthCheckState.Healthy
			this.message = message
		}
	}

	public suspend fun startingIf(message: String? = null, body: suspend () -> Boolean) {
		if (this.state != HealthCheckState.Starting && body()) {
			this.state = HealthCheckState.Starting
			this.message = message
		}
	}

	public suspend fun startingIfNot(message: String? = null, body: suspend () -> Boolean) {
		if (this.state != HealthCheckState.Starting && !body()) {
			this.state = HealthCheckState.Starting
			this.message = message
		}
	}

	public suspend fun unhealthyIf(message: String? = null, body: suspend () -> Boolean) {
		if (this.state != HealthCheckState.Unhealthy && body()) {
			this.state = HealthCheckState.Unhealthy
			this.message = message
		}
	}

	public suspend fun unhealthyIfNot(message: String? = null, body: suspend () -> Boolean) {
		if (this.state != HealthCheckState.Unhealthy && !body()) {
			this.state = HealthCheckState.Unhealthy
			this.message = message
		}
	}
}
