/*
 * Copyrighted (Kord Extensions, 2026). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.components.forms.widgets

import dev.kord.common.entity.DiscordSelectOption
import dev.kord.rest.builder.component.LabelComponentBuilder
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.i18n.Key
import java.util.*

/** The min number of options for the radio group widget. **/
private const val MIN_OPTIONS: Int = 2

/** The max number of options for the radio group widget. **/
private const val MAX_OPTIONS: Int = 10

/** A widget for selecting exactly one option from a defined list. **/
public class RadioGroupWidget : Widget<String?>(), KordExKoinComponent {
	@Suppress("MagicNumber")
	override var height: Int = 5
	override var width: Int = 1
	override var value: String? = null

	/** The widget's unique ID on Discord, defaulting to a UUID. **/
	public var id: String = UUID.randomUUID().toString()

	public override lateinit var label: Key

	public override var description: Key? = null

	/** The list of options to show to the user. **/
	public lateinit var options: List<DiscordSelectOption>

	/** Whether a selection is required to submit the modal. **/
	public val required: Boolean = true

	public override fun validate() {
		if (this::options.isInitialized.not() || options.isEmpty()) {
			error("Options cannot be empty. Must contain between $MIN_OPTIONS and $MAX_OPTIONS options!")
		}

		if (options.size !in MIN_OPTIONS..MAX_OPTIONS) {
			error("Invalid number of options! expected $MIN_OPTIONS - $MAX_OPTIONS options!")
		}
	}

	public override suspend fun apply(builder: LabelComponentBuilder, locale: Locale) {
		val translatedDescription = description
			?.withLocale(locale)
			?.translate()

		builder.description = translatedDescription

		builder.radioGroup(id) {
			this.options = this@RadioGroupWidget.options
			this.required = this@RadioGroupWidget.required
		}
	}

	/** @suppress Internal API method. **/
	@JvmName("setValue1")
	public fun setValue(value: String?) {
		this.value = value
	}
}
