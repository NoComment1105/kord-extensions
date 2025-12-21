/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.i18n

import dev.kordex.core.types.TranslatableContext
import dev.kordex.i18n.Key

public suspend fun Key.withContext(context: TranslatableContext): Key =
	withLocale(context.getLocale())
