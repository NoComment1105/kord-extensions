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

/** A checkbox widget that supports multiple checkboxes. **/
public class CheckboxGroupWidget : Widget<List<String>>(), KordExKoinComponent {
	@Suppress("MagicNumber")
	override var width: Int = 5
	override var height: Int = 1
	override var value: List<String> = emptyList()

	/** The widget's unique ID on Discord, defaulting to a UUID. **/
	public var id: String = UUID.randomUUID().toString()

	/** The list of options to show. **/
	public lateinit var options: List<DiscordSelectOption>

	/** Whether this widget must be filled out for the form to be valid. **/
	public var required: Boolean = true

	/** The minimum number of items that must be chosen. **/
	public var minValues: Int = if (required) MIN_VALUES + 1 else MIN_VALUES

	/** The maximum number of items that can be chosen. **/
	public var maxValues: Int = MAX_VALUES

	public override fun validate() {
		if (this::options.isInitialized.not() || options.isEmpty()) {
			error("You must provide options for the checkbox group!")
		}

		@Suppress("UnnecessaryParentheses")
		if (options.size !in (MIN_VALUES + 1)..MAX_VALUES) {
			error("Invalid number of options provided: ${options.size} - expected ${MIN_VALUES + 1} - $MAX_VALUES")
		}

		if (maxValues < minValues) {
			error("maxValues cannot be less than minValues!")
		}

		@Suppress("UnnecessaryParentheses")
		if (maxValues !in (MIN_VALUES + 1)..MAX_VALUES) {
			error(
				"Invalid value for maxLength provided: $maxValues - expected ${MIN_VALUES + 1} - $MAX_VALUES"
			)
		}

		if (minValues !in MIN_VALUES until MAX_VALUES) {
			error(
				"Invalid value for minLength provided: $minValues - expected $MIN_VALUES - ${MAX_VALUES - 1}"
			)
		}
	}

	public override suspend fun apply(builder: LabelComponentBuilder, locale: Locale) {
		builder.checkboxGroup(id) {
			this.options = this@CheckboxGroupWidget.options
			this.minValues = this@CheckboxGroupWidget.minValues
			this.maxValues = this@CheckboxGroupWidget.maxValues
			this.required = this@CheckboxGroupWidget.required
		}
	}

	/** @suppress Internal API method. **/
	@JvmName("setValue1")
	public fun setValue(value: List<String>) {
		this.value = value
	}
}
