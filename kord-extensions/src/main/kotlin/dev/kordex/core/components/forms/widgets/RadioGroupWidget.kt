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
import java.util.*

public class RadioGroupWidget : Widget<String?>(), KordExKoinComponent {
	@Suppress("MagicNumber")
	override var height: Int = 5
	override var width: Int = 1
	override var value: String? = null

	public var id: String = UUID.randomUUID().toString()

	public lateinit var options: List<DiscordSelectOption>

	public val required: Boolean = true

	public override fun validate() {
		if (this::options.isInitialized.not() || options.isEmpty()) {
			error("Options cannot be empty. Must contain between 2 and 10 options!")
		}

		if (options.size !in 2..10) {
			error("Invalid number of options! expected 2 - 10 options!")
		}
	}

	public override suspend fun apply(builder: LabelComponentBuilder, locale: Locale) {
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
