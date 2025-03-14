/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.test.bot.extensions

import dev.kordex.core.extensions.Extension
import dev.kordex.core.healthcheck.HealthCheckState
import dev.kordex.core.healthcheck.utils.addHealthCheck

public class MiscExtension : Extension() {
	override val name: String = "kordex.test-misc"

	override suspend fun setup() {
		addHealthCheck("always-fails") {
			state(HealthCheckState.Unhealthy)
		}
	}
}
