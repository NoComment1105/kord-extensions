/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:Suppress("StringLiteralDuplication")

package dev.kordex.core.utils

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.execute
import dev.kord.core.behavior.executeIgnored
import dev.kord.core.entity.Message
import dev.kord.core.entity.Webhook
import dev.kord.rest.builder.message.create.WebhookMessageCreateBuilder
import dev.kord.rest.builder.webhook.WebhookModifyBuilder
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/** @suppress **/
public const val ERR_TOKEN_NULL: String = "Webhook token is null"

/**
 * Edit this webhook using the token stored within the object.
 *
 * **Note:** We will deprecate and remove this function if Kord implements their own.
 *
 * @return The updated [Webhook].
 *
 * @throws [IllegalStateException] if the webhook object doesn't contain a token.
 * @throws [dev.kord.rest.request.RestRequestException] if something went wrong during the request.
 */
public suspend inline fun Webhook.editStored(
	builder: WebhookModifyBuilder.() -> Unit,
): Webhook {
	contract {
		callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
	}

	if (token == null) {
		error(ERR_TOKEN_NULL)
	}

	return edit(token!!, builder)
}

/**
 * Execute this webhook using the token stored within the object.
 * If [threadId] is provided, execute the webhook in that thread.
 *
 * **Note:** We will deprecate and remove this function if Kord implements their own.
 *
 * @return The newly-created [Message].
 *
 * @throws [IllegalStateException] if the webhook object doesn't contain a token.
 * @throws [dev.kord.rest.request.RestRequestException] if something went wrong during the request.
 */
public suspend inline fun Webhook.executeStored(
	threadId: Snowflake? = null,
	withComponents: Boolean? = null,
	builder: WebhookMessageCreateBuilder.() -> Unit,
): Message {
	contract {
		callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
	}

	if (token == null) {
		error(ERR_TOKEN_NULL)
	}

	return execute(token!!, threadId, withComponents, builder)
}

/**
 * Execute this webhook using the token stored within the object, ignoring the response.
 * If [threadId] is provided, execute the webhook in that thread.
 *
 * This is a "fire-and-forget" variant of [execute].
 * It won't wait for a response and may not throw an exception if the request fails.
 *
 * **Note:** We will deprecate and remove this function if Kord implements their own.
 *
 * @throws [IllegalStateException] if the webhook object doesn't contain a token.
 * @throws [dev.kord.rest.request.RestRequestException] if something went wrong during the request.
 */
public suspend inline fun Webhook.executeStoredIgnored(
	threadId: Snowflake? = null,
	withComponents: Boolean? = null,
	builder: WebhookMessageCreateBuilder.() -> Unit,
) {
	contract {
		callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
	}

	if (token == null) {
		error(ERR_TOKEN_NULL)
	}

	executeIgnored(token!!, threadId, withComponents, builder)
}
