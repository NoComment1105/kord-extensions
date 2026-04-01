/*
 * Copyrighted (Kord Extensions, 2026). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.components.forms.widgets.menus

import dev.kord.rest.builder.component.ActionRowBuilder
import dev.kord.rest.builder.component.SelectOptionBuilder
import dev.kordex.core.components.forms.widgets.MIN_LENGTH
import java.util.Locale

/** A select widget that supports strings as options. **/
public class StringSelectMenuWidget : SelectMenuWidget<String, StringSelectMenuWidget>() {
	/** Specified choices in a select menu.  **/
	public val options: MutableList<SelectOptionBuilder> = mutableListOf()

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

		builder.stringSelect(id) {
			this.options = this@StringSelectMenuWidget.options
			this.allowedValues = this@StringSelectMenuWidget.minValues..this@StringSelectMenuWidget.maxValues
			// Wait for Kord to expose this before uncommenting
			// this.required = this@StringSelectMenuWidget.required
			this.placeholder = translatedPlaceholder
		}
	}
}
