/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.healthcheck

import dev.kordex.core.i18n.generated.CoreTranslations.Extensions.Sentry.Arguments.id
import dev.kordex.core.utils.scheduling.Scheduler
import io.sentry.MonitorSchedule.interval
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class HealthCheckRegistry {
	public val checks: MutableSet<HealthCheck> = mutableSetOf<HealthCheck>()
	public var scheduler: Scheduler? = null

	public var running: Boolean = false

	public fun add(check: HealthCheck) {
		checks.add(check)

		if (running) {
			check.start(scheduler!!)
		}
	}

	public fun addAnonymous(
		id: String,
		interval: Duration = 30.seconds,
		callback: suspend HealthCheckResult.() -> Unit
	): HealthCheck {
		val check = HealthCheck(
			extension = null,
			id = id,
			interval = interval,
			callback = callback
		)

		add(check)

		return check
	}

	public fun remove(check: HealthCheck) {
		check.stop()

		checks.remove(check)
	}

	public fun start() {
		scheduler = Scheduler()

		checks.forEach { it.start(scheduler!!) }

		running = true
	}

	public fun shutdown() {
		running = false

		checks.forEach { it.stop() }

		scheduler?.shutdown()
		scheduler = null
	}
}
