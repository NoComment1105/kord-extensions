/*
 * Copyrighted (Kord Extensions, 2026). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.components.forms.widgets

import dev.kord.rest.builder.component.LabelComponentBuilder
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.i18n.Key
import java.util.*

/** A checkbox widget that supports a single checkbox. **/
public class CheckboxWidget : Widget<Boolean?>(), KordExKoinComponent {
	@Suppress("MagicNumber")
	override var width: Int = 5
	override var height: Int = 1
	override var value: Boolean? = null

	/** The widget's unique ID on Discord, defaulting to a UUID. **/
	public var id: String = UUID.randomUUID().toString()

	public override lateinit var label: Key

	public override var description: Key? = null

	/** Whether the checkbox is selected by default. **/
	public var default: Boolean = false

	public override fun validate() {
		if (this::label.isInitialized.not() || label.key.isEmpty()) {
			error("Checkboxes must be given a label, but no label was provided")
		}
	}

	public override suspend fun apply(builder: LabelComponentBuilder, locale: Locale) {
		val translatedDescription = description
			?.withLocale(locale)
			?.translate()

		builder.description = translatedDescription

		builder.checkbox(id) {
			this.default = this@CheckboxWidget.default
		}
	}

	/** @suppress Internal API method. **/
	@JvmName("setValue1")
	public fun setValue(value: Boolean) {
		this.value = value
	}
}
