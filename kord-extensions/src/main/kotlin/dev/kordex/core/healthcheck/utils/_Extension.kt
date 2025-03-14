/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.healthcheck.utils

import dev.kordex.core.annotations.InternalAPI
import dev.kordex.core.extensions.Extension
import dev.kordex.core.healthcheck.HealthCheck
import dev.kordex.core.healthcheck.HealthCheckRegistry
import dev.kordex.core.healthcheck.HealthCheckResult
import dev.kordex.core.healthcheck.HealthCheckState
import dev.kordex.core.utils.getKoin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val registry: HealthCheckRegistry
	get() = getKoin().get<HealthCheckRegistry>()

@OptIn(InternalAPI::class)
public suspend fun Extension.addHealthCheck(
	id: String,
	interval: Duration = 30.seconds,
	body: suspend HealthCheckResult.() -> Unit,
): HealthCheck {
	val check = HealthCheck(
		extension = this.name,
		id = id,
		interval = interval,
		callback = body
	)

	unloadCallbacks.add {
		registry.remove(check)
	}

	registry.add(check)

	return check
}

@OptIn(InternalAPI::class)
public fun Extension.healthCheckState(type: String, ordinal: Int): HealthCheckState {
	val state = HealthCheckState.addState(type, ordinal)

	unloadCallbacks.add {
		HealthCheckState.removeState(type)
	}

	return state
}
