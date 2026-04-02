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
import java.util.*

public class CheckboxWidget : Widget<Boolean?>(), KordExKoinComponent {
	@Suppress("MagicNumber")
	override var width: Int = 5
	override var height: Int = 1
	override var value: Boolean? = null

	/** The widget's unique ID on Discord, defaulting to a UUID. **/
	public var id: String = UUID.randomUUID().toString()

	public var default: Boolean = false

	public override fun validate() { }

	public override suspend fun apply(builder: LabelComponentBuilder, locale: Locale) {
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
