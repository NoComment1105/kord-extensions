/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.healthcheck

import dev.kordex.core.utils.scheduling.Scheduler
import dev.kordex.core.utils.scheduling.Task
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class HealthCheck(
	public val extension: String?,
	public val id: String,
	public val interval: Duration = 30.seconds,

	public val callback: suspend HealthCheckResult.() -> Unit,
) {
	internal var task: Task? = null

	public val isStarted: Boolean
		get() = task != null

	public var lastState: HealthCheckState = HealthCheckState.Starting
		private set

	public var lastMessage: String? = null
		private set

	@Suppress("TooGenericExceptionCaught")
	public suspend fun execute() {
		try {
			val result = HealthCheckResult(lastState, lastMessage)

			callback(result)

			lastState = result.state
			lastMessage = result.message
		} catch (e: Exception) {
			lastState = HealthCheckState.Unhealthy
			lastMessage = e.message
		}
	}

	public fun start(scheduler: Scheduler) {
		scheduler.launch {
			task = scheduler.schedule(
				callback = ::execute,
				delay = interval,
				name = "Health Check: $extension -> $id",
				repeat = true,
				startNow = true,
			)
		}

		scheduler.launch {
			execute()
		}
	}

	public fun stop() {
		if (task != null) {
			task!!.cancel()
		}
	}
}
