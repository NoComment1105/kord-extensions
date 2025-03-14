/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.modules.web.core.backend.utils

import dev.kord.core.Kord
import dev.kordex.core.builders.ExtensibleBotBuilder
import dev.kordex.core.healthcheck.HealthCheckRegistry
import dev.kordex.core.healthcheck.HealthCheckState
import dev.kordex.core.utils.getKoin
import kotlinx.serialization.Serializable

public fun HealthCheckRegistry.summarize(): HealthCheckSummary {
	val botBuilder = getKoin().get<ExtensibleBotBuilder>()
	val registry = getKoin().get<HealthCheckRegistry>()
	val kord = getKoin().get<Kord>()

	val overallState = registry.checks
		.minBy { it.lastState.ordinal }
		.lastState

	val anonymousStates = registry.checks
		.filter { it.extension == null }
		.map {
			HealthCheckAnonymous(
				id = it.id,
				status = it.lastState,
				message = it.lastMessage
			)
		}

	val extensionStates = registry.checks
		.filter { it.extension != null }
		.groupBy { it.extension }
		.mapValues { it.value.minBy { it.lastState.ordinal } }
		.map { (extension, check) ->
			HealthCheckExtension(
				extension = extension!!,
				id = check.id,
				status = check.lastState,
				message = check.lastMessage
			)
		}

	return HealthCheckSummary(
		version = botBuilder.botVersion,
		status = overallState,

		averagePing = kord.gateway.averagePing?.inWholeMilliseconds,
		shardPings = kord.gateway.gateways.mapValues { it.value.ping.value?.inWholeMilliseconds },

		extensions = extensionStates,
		statuses = anonymousStates,
	)
}

@Serializable
public data class HealthCheckSummary(
	val version: String? = null,
	val status: HealthCheckState,

	val averagePing: Long?,
	val shardPings: Map<Int, Long?>,

	val extensions: List<HealthCheckExtension>,
	val statuses: List<HealthCheckAnonymous>,
)

@Serializable
public data class HealthCheckExtension(
	val extension: String,
	val id: String,
	val status: HealthCheckState,
	val message: String? = null,
)

@Serializable
public data class HealthCheckAnonymous(
	val id: String,
	val status: HealthCheckState,
	val message: String? = null,
)
