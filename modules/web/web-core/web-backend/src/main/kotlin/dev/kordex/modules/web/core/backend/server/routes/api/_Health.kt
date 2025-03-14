/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.modules.web.core.backend.server.routes.api

import dev.kordex.core.healthcheck.HealthCheckRegistry
import dev.kordex.core.utils.getKoin
import dev.kordex.modules.web.core.backend.utils.summarize
import io.ktor.server.response.*
import io.ktor.server.routing.*

public fun Route.health() {
	get("/health") {
		val registry = getKoin().get<HealthCheckRegistry>()

		call.respond(registry.summarize())
	}
}
