/*
 * Copyrighted (Kord Extensions, 2026). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.components.forms.widgets.menus

import dev.kord.rest.builder.component.LabelComponentBuilder
import dev.kord.rest.builder.component.SelectOptionBuilder
import dev.kordex.core.components.forms.widgets.MIN_LENGTH
import dev.kordex.core.components.menus.string.StringSelectMenu
import java.util.Locale

/** A select widget that supports strings as options. **/
public class StringSelectMenuWidget : SelectMenuWidget<String, StringSelectMenuWidget>(), StringSelectMenu {
	/** Specified choices in a select menu.  **/
	public override val options: MutableList<SelectOptionBuilder> = mutableListOf()

	override suspend fun apply(builder: LabelComponentBuilder, locale: Locale) {
		val translatedDescription = description
			?.withLocale(locale)
			?.translate()

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
		builder.description = translatedDescription

		builder.stringSelect(id) {
			this.options = this@StringSelectMenuWidget.options
			this.allowedValues = this@StringSelectMenuWidget.minValues..this@StringSelectMenuWidget.maxValues
			this.required = this@StringSelectMenuWidget.required
			this.placeholder = translatedPlaceholder
		}
	}
}
