/*
 * Copyrighted (Kord Extensions, 2026). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.components.forms.widgets.menus

import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.component.LabelComponentBuilder
import dev.kordex.core.components.forms.widgets.MIN_LENGTH
import dev.kordex.core.components.menus.mentionable.MentionableSelectMenu
import java.util.Locale

/** A select widget that supports mentionables as options. **/
public class MentionableSelectMenuWidget :
    SelectMenuWidget<Snowflake, MentionableSelectMenuWidget>(), MentionableSelectMenu {
	public override var defaultUsers: MutableList<Snowflake> = mutableListOf()

	public override var defaultRoles: MutableList<Snowflake> = mutableListOf()

	override fun validate() {
		super.validate()

		val defaultTotals = defaultRoles.size + defaultUsers.size

		if (defaultTotals > maxValues) {
			error(
				"The number of default values set ($defaultTotals) is greater than the maximum number of values" +
					" that can be selected! ($maxValues)"
			)
		}
	}

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

		builder.mentionableSelect(id) {
			this.defaultUsers.addAll(this@MentionableSelectMenuWidget.defaultUsers)
			this.defaultRoles.addAll(this@MentionableSelectMenuWidget.defaultRoles)
			this.allowedValues = this@MentionableSelectMenuWidget.minValues..this@MentionableSelectMenuWidget.maxValues
			this.required = this@MentionableSelectMenuWidget.required
			this.placeholder = translatedPlaceholder
		}
	}
}
