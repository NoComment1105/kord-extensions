/*
 * Copyrighted (Kord Extensions, 2026). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.components.forms.widgets

import dev.kord.common.entity.Snowflake
import dev.kord.rest.builder.component.LabelComponentBuilder
import dev.kordex.core.koin.KordExKoinComponent
import java.util.Locale
import java.util.UUID

public const val MIN_VALUES: Int = 0

public const val MAX_VALUES: Int = 10

public class FileUploadWidget : Widget<List<Snowflake>?>(), KordExKoinComponent {
	@Suppress("MagicNumber")
	override var width: Int = 5
	override var height: Int = 1
	override var value: List<Snowflake>? = null

	/** The widget's unique ID on Discord, defaulting to a UUID. **/
	public var id: String = UUID.randomUUID().toString()

	/** Whether this widget must be filled out for the form to be valid. **/
	public var required: Boolean = true

	/** The minimum number of items that must be uploaded. If [required] is omitted/true, set to 1. Otherwise 0. **/
	public var minValues: Int = if (required) MIN_VALUES + 1 else MIN_VALUES

	/** The maximum number of items that can be uploaded. **/
	public var maxValues: Int = MAX_VALUES

	public override fun validate() {
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
		builder.fileUpload(id) {
			this.minValues = this@FileUploadWidget.minValues
			this.maxValues = this@FileUploadWidget.maxValues
			this.required = this@FileUploadWidget.required
		}
	}

	/** @suppress Internal API method. **/
	@JvmName("setValue1")
	public fun setValue(value: List<Snowflake>) {
		this.value = value
	}
}
