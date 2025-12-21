/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.test.bot.extensions

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.role
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.publicSlashCommand
import dev.kordex.core.healthcheck.HealthCheckState
import dev.kordex.core.healthcheck.utils.addHealthCheck
import dev.kordex.i18n.toKey
import dev.kordex.modules.web.core.backend.utils.apiRoutes
import io.ktor.resources.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

public class MiscExtension : Extension() {
	override val name: String = "kordex.test-misc"

	override suspend fun setup() {
		apiRoutes {
			get("/test/basic") {
				call.respond("It works!")
			}

			get<Params> { params ->
				call.respond("Parameter: ${params.param}")
			}

			println("Registered route.")
		}

		addHealthCheck("always-fails") {
			state(HealthCheckState.Unhealthy)
		}

		publicSlashCommand(::RoleArgs) {
			name = "break-perms".toKey()
			description = "Give a role admin perms at channel level".toKey()

			check { hasPermission(Permission.Administrator) }

			action {
				val c = channel.asChannelOf<TopGuildMessageChannel>()

				c.editRolePermission(arguments.role.id) {
					val existing = c.permissionOverwrites.find { it.target == arguments.role.id }?.allowed

					if (existing != null) {
						allowed += existing
					}

					allowed += Permission.CreateGuildExpressions
					allowed += Permission.CreateEvents
				}

				respond {
					content = "Role ${arguments.role.mention} now has the Create Events and Create Expressions perms in this channel."
				}
			}
		}

		ephemeralSlashCommand(::AAAArgs) {
			name = "aaaa".toKey()
			description = "Should always fail".toKey()

			action {
				respond {
					content = "If you see this, validators borked."
				}
			}
		}
	}
}

public class RoleArgs : Arguments() {
	public val role: Role by role {
		name = "role".toKey()
		description = "Role to mess with".toKey()
	}
}

public class AAAArgs : Arguments() {
	public val url: String by string {
		name = "url".toKey()
		description = "URL".toKey()

		validate {
			fail()
		}
	}
}

@Resource("/test/params")
public data class Params(
	val param: String? = null
)
