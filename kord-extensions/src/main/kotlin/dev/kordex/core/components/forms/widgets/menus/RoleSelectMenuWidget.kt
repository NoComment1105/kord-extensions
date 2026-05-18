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
import dev.kordex.core.components.menus.role.RoleSelectMenu
import java.util.Locale

/** A select widget that supports roles as options. **/
public class RoleSelectMenuWidget : SelectMenuWidget<Snowflake, RoleSelectMenuWidget>(), RoleSelectMenu {
	public override var defaultRoles: MutableList<Snowflake> = mutableListOf()

	override fun validate() {
		super.validate()

		if (defaultRoles.size > maxValues) {
			error(
				"The number of default values set (${defaultRoles.size}) is greater than the maximum number of values" +
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

		builder.roleSelect(id) {
			this.defaultRoles.addAll(this@RoleSelectMenuWidget.defaultRoles)
			this.allowedValues = this@RoleSelectMenuWidget.minValues..this@RoleSelectMenuWidget.maxValues
			this.required = this@RoleSelectMenuWidget.required
			this.placeholder = translatedPlaceholder
		}
	}
}
