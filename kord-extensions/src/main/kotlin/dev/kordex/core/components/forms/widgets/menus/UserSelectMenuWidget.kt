/*
 * Copyrighted (Kord Extensions, 2026). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.components.forms.widgets.menus

import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kordex.core.components.forms.widgets.MIN_LENGTH
import java.util.*

/** A select widget that supports users as options. **/
public class UserSelectMenuWidget : SelectMenuWidget<Snowflake, UserSelectMenuWidget>() {
	/** Default values for autopopulated select menu components. **/
	public val defaultUsers: MutableList<Snowflake> = mutableListOf()

	override suspend fun apply(builder: ActionRowBuilder, locale: Locale) {
		val translatedPlaceholder = placeholder
			?.withLocale(locale)
			?.translate()

		if (
			translatedPlaceholder != null &&
			(translatedPlaceholder.length > SELECT_PLACEHOLDER_LENGTH || translatedPlaceholder.isEmpty())
		) {
			error(
				"Invalid value for placeholder provided (${translatedPlaceholder.length} characters) - expected " +
					"${MIN_LENGTH + 1} - $SELECT_PLACEHOLDER_LENGTH characters"
			)
		}

		builder.userSelect(id) {
			this.defaultUsers.addAll(this@UserSelectMenuWidget.defaultUsers)
			this.allowedValues = this@UserSelectMenuWidget.minValues..this@UserSelectMenuWidget.maxValues
			// Wait for Kord to expose this before uncommenting
			// this.required = this@UserSelectMenuWidget.required
			this.placeholder = translatedPlaceholder
		}
	}
}
