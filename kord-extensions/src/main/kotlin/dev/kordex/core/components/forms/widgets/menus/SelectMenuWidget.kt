/*
 * Copyrighted (Kord Extensions, 2026). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.components.forms.widgets.menus

import dev.kordex.core.components.forms.widgets.MAX_LENGTH
import dev.kordex.core.components.forms.widgets.MIN_LENGTH
import dev.kordex.core.components.forms.widgets.MIN_VALUES
import dev.kordex.core.components.forms.widgets.Widget
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.i18n.Key
import java.util.*

/** The absolute max number of items that can be chosen. **/
public const val MAX_VALUES: Int = 25

/** The maximum number of characters that can be present in the select widget's placeholder. **/
public const val SELECT_PLACEHOLDER_LENGTH: Int = 150

public abstract class SelectMenuWidget<C, T : SelectMenuWidget<C, T>> : Widget<List<C?>>(), KordExKoinComponent {

	@Suppress("MagicNumber")
	override var width: Int = 5
	override var height: Int = 1
	override var value: List<C?> = emptyList()

	public override lateinit var label: Key

	public override var description: Key? = null

	/** The widget's unique ID on Discord, defaulting to a UUID. **/
	public var id: String = UUID.randomUUID().toString()

	/** The minimum number of items that must be chosen. Default: 1. **/
	public var minValues: Int = 1

	/** The maximum number of items that can be chosen. Default: 1. **/
	public var maxValues: Int = 1

	/** Placeholder text if nothing is selected, to be shown to the user on Discord. **/
	public var placeholder: Key? = null

	/** Whether this widget must be filled out for the form to be valid. **/
	public var required: Boolean = true

	public override fun validate() {
		if (this::label.isInitialized.not() || label.key.isEmpty()) {
			error("Widgets must be given a label, but no label was provided.")
		}

		if (maxValues < minValues) {
			error("maxValues cannot be less than minValues!")
		}

		@Suppress("UnnecessaryParentheses")
		if (maxValues !in (MIN_VALUES + 1)..MAX_VALUES) {
			error(
				"Invalid value for maxValues provided: $maxValues - expected ${MIN_LENGTH + 1} - $MAX_LENGTH"
			)
		}

		if (minValues !in MIN_VALUES until MAX_VALUES) {
			error(
				"Invalid value for minValues provided: $minValues - expected $MIN_VALUES - ${MAX_VALUES - 1}"
			)
		}
	}

	/** @suppress Internal API method. **/
	@JvmName("setValue1")
	public fun setValue(value: List<C>) {
		this.value = value
	}
}
