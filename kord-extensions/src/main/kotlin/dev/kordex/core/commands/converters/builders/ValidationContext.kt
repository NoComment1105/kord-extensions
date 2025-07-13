/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.commands.converters.builders

import dev.kord.core.event.Event
import dev.kordex.core.DiscordRelayedException
import dev.kordex.core.checks.types.CheckContext
import dev.kordex.core.commands.CommandContext
import java.util.*

/**
 * Class representing the context for an argument validator. This allows the storage of validation steps and a message
 * for the user.
 *
 * @property T TypeVar representing the current argument type
 * @property value Value of type [T]
 * @property context Command context that triggered this validation
 */
public class ValidationContext<out T>(
	public val value: T,
	public val context: CommandContext,
	locale: Locale,
) : CheckContext<Event>(context.eventObj, locale) {
	/**
	 * If this validator has failed, throw a [DiscordRelayedException] with the translated message, if any.
	 */
	@Throws(DiscordRelayedException::class)
	public fun throwIfFailed() {
		if (passed.not()) {
			val key = getMessageKey()

			if (key != null) {
				throw DiscordRelayedException(key)
			} else {
				error("Validation failed.")
			}
		}
	}
}
