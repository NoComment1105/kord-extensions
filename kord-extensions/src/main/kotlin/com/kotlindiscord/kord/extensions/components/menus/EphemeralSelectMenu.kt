@file:Suppress("TooGenericExceptionCaught")

package com.kotlindiscord.kord.extensions.components.menus

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.interactions.respond
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent
import dev.kord.rest.builder.message.create.EphemeralInteractionResponseCreateBuilder

public typealias InitialEphemeralSelectMenuResponseBuilder =
    (suspend EphemeralInteractionResponseCreateBuilder.(SelectMenuInteractionCreateEvent) -> Unit)?

/** Class representing an ephemeral-only select (dropdown) menu. **/
public open class EphemeralSelectMenu : SelectMenu<EphemeralSelectMenuContext>() {
    /** @suppress Initial response builder. **/
    public open var initialResponseBuilder: InitialEphemeralSelectMenuResponseBuilder = null

    /** Call this to open with a response, omit it to ack instead. **/
    public fun initialResponse(body: InitialEphemeralSelectMenuResponseBuilder) {
        initialResponseBuilder = body
    }

    override suspend fun call(event: SelectMenuInteractionCreateEvent) {
        try {
            if (!runChecks(event)) {
                return
            }
        } catch (e: CommandException) {
            event.interaction.respondEphemeral { content = e.reason }

            return
        }

        val response = if (initialResponseBuilder != null) {
            event.interaction.respondEphemeral { initialResponseBuilder!!(event) }
        } else {
            if (!deferredAck) {
                event.interaction.acknowledgeEphemeralDeferredMessageUpdate()
            } else {
                event.interaction.acknowledgeEphemeral()
            }
        }

        val context = EphemeralSelectMenuContext(this, event, response)

        context.populate()

        firstSentryBreadcrumb(context, this)

        try {
            checkBotPerms(context)
            body(context)
        } catch (e: CommandException) {
            respondText(context, e.reason)
        } catch (t: Throwable) {
            handleError(context, t, this)
        }
    }

    override suspend fun respondText(context: EphemeralSelectMenuContext, message: String) {
        context.respond { content = message }
    }
}
