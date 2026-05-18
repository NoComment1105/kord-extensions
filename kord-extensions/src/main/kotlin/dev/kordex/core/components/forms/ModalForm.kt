/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:OptIn(KordUnsafe::class)

package dev.kordex.core.components.forms

import dev.kord.common.annotation.KordUnsafe
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.ModalParentInteractionBehavior
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.EphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.PublicMessageInteractionResponseBehavior
import dev.kord.core.entity.interaction.ModalSubmitInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.interaction.ModalBuilder
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.components.ComponentContext
import dev.kordex.core.components.ComponentRegistry
import dev.kordex.core.components.forms.widgets.*
import dev.kordex.core.components.forms.widgets.menus.*
import dev.kordex.core.events.EventContext
import dev.kordex.core.events.ModalInteractionCompleteEvent
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.utils.waitFor
import dev.kordex.i18n.Key
import org.koin.core.component.inject
import java.util.*
import kotlin.collections.flatten
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Class representing a modal form.
 *
 * This should be extended by classes representing individual modals.
 */
public abstract class ModalForm : Form(), KordExKoinComponent {
	/** The modal's title, shown on Discord. **/
	public abstract var title: Key

	/** @suppress Internal reference. **/
	protected val bot: ExtensibleBot by inject()

	/** @suppress Internal reference. **/
	protected val componentRegistry: ComponentRegistry by inject()

	override val timeout: Duration = 15.minutes

	/** ID representing this modal on Discord. **/
	public var id: String = UUID.randomUUID().toString()

	/** A widget representing a single-line text input. **/
	public fun lineText(
		coordinate: CoordinatePair? = null,
		builder: LineTextWidget.() -> Unit,
	): LineTextWidget {
		val widget = LineTextWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	/** A widget representing a multi-line paragraph input. **/
	public fun paragraphText(
		coordinate: CoordinatePair? = null,
		builder: ParagraphTextWidget.() -> Unit,
	): ParagraphTextWidget {
		val widget = ParagraphTextWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	public fun channelSelect(
		coordinate: CoordinatePair? = null,
		builder: ChannelSelectMenuWidget.() -> Unit,
	): ChannelSelectMenuWidget {
		val widget = ChannelSelectMenuWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	public fun mentionableSelect(
		coordinate: CoordinatePair? = null,
		builder: MentionableSelectMenuWidget.() -> Unit,
	): MentionableSelectMenuWidget {
		val widget = MentionableSelectMenuWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	public fun roleSelect(
		coordinate: CoordinatePair? = null,
		builder: RoleSelectMenuWidget.() -> Unit,
	): RoleSelectMenuWidget {
		val widget = RoleSelectMenuWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	public fun stringSelect(
		coordinate: CoordinatePair? = null,
		builder: StringSelectMenuWidget.() -> Unit,
	): StringSelectMenuWidget {
		val widget = StringSelectMenuWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	public fun userSelect(
		coordinate: CoordinatePair? = null,
		builder: UserSelectMenuWidget.() -> Unit,
	): UserSelectMenuWidget {
		val widget = UserSelectMenuWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	public fun fileUpload(
		coordinate: CoordinatePair? = null,
		builder: FileUploadWidget.() -> Unit,
	): FileUploadWidget {
		val widget = FileUploadWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	public fun radioGroup(
		coordinate: CoordinatePair? = null,
		builder: RadioGroupWidget.() -> Unit,
	): RadioGroupWidget {
		val widget = RadioGroupWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	public fun checkboxGroup(
		coordinate: CoordinatePair? = null,
		builder: CheckboxGroupWidget.() -> Unit,
	): CheckboxGroupWidget {
		val widget = CheckboxGroupWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	public fun checkbox(
		coordinate: CoordinatePair? = null,
		builder: CheckboxWidget.() -> Unit,
	): CheckboxWidget {
		val widget = CheckboxWidget()

		builder(widget)
		widget.validate()

		grid.setAtCoordinateOrFirstRow(coordinate, widget)

		return widget
	}

	/** @suppress Internal function called by the component registry. **/
	public suspend fun call(event: ModalSubmitInteractionCreateEvent) {
		val interaction = event.interaction

		grid.filter { it.isNotEmpty() }.flatten().forEach { widget -> getWidgetValue(widget, interaction)  }

		bot.send(ModalInteractionCompleteEvent(id, interaction))
	}

	private fun getWidgetValue(widget: Widget<*>?, interaction: ModalSubmitInteraction) {
		when (widget) {
			is TextInputWidget<*> -> interaction.textInputs[widget.id]?.value?.let(widget::setValue)

			is ChannelSelectMenuWidget ->
				interaction.channelSelects[widget.id]?.values.stringListToSnowflakeList()?.let(widget::setValue)

			is MentionableSelectMenuWidget ->
				interaction.mentionableSelects[widget.id]?.values.stringListToSnowflakeList()?.let(widget::setValue)

			is RoleSelectMenuWidget ->
				interaction.roleSelects[widget.id]?.values.stringListToSnowflakeList()?.let(widget::setValue)

			is StringSelectMenuWidget -> interaction.stringSelects[widget.id]?.values?.let(widget::setValue)

			is UserSelectMenuWidget ->
				interaction.userSelects[widget.id]?.values.stringListToSnowflakeList()?.let(widget::setValue)

			is FileUploadWidget -> interaction.fileUploads[widget.id]?.valueIds?.let(widget::setValue)

			is RadioGroupWidget -> interaction.radioGroups[widget.id]?.value?.let(widget::setValue)

			is CheckboxGroupWidget -> interaction.checkboxGroups[widget.id]?.values?.let(widget::setValue)

			is CheckboxWidget -> interaction.checkboxes[widget.id]?.value?.let(widget::setValue)
		}
	}

	/** Given a ModalBuilder, apply this modal's widgets for display on Discord. **/
	public suspend fun applyToBuilder(builder: ModalBuilder, locale: Locale) {
		val appliedWidgets = mutableSetOf<Widget<*>>()

		grid.forEach { row ->
			val filteredRow = row.filterNotNull()
				.filter { it !in appliedWidgets }

			if (filteredRow.isNotEmpty()) {
				filteredRow.forEach { widget ->
					if (widget !in appliedWidgets) {
						builder.label(widget.label.withLocale(locale).translate()) {
							widget.apply(this, locale)
							appliedWidgets.add(widget)
						}
					}
				}
			}
		}
	}

	/** Wait for this modal to be completed and call the [callback]. Parameter will be `null` if timed out. **/
	public suspend fun <T> awaitCompletion(callback: suspend (interaction: ModalSubmitInteraction?) -> T): T {
		val completionEvent = bot.waitFor<ModalInteractionCompleteEvent>(timeout) { id == this@ModalForm.id }

		return callback(completionEvent?.interaction)
	}

	/** Return a translated modal title using the given locale. **/
	public fun translateTitle(locale: Locale): String =
		title
			.withLocale(locale)
			.translate()

	/**
	 * Convenience function to send this modal to the given [interaction] and await its completion, running the provided
	 * [callback].
	 *
	 * `null` will be provided to the callback if the modal times out before the user responds.
	 *
	 * More specific convenience functions are available, such as [sendAndDeferEphemeral] and [sendAndDeferPublic].
	 */
	public suspend fun <T> sendAndAwait(
		locale: Locale,
		interaction: ModalParentInteractionBehavior,
		callback: suspend (interaction: ModalSubmitInteraction?) -> T,
	): T {
		componentRegistry.register(this)

		interaction.modal(translateTitle(locale), id) {
			applyToBuilder(this, locale)
		}

		return awaitCompletion(callback)
	}

	/**
	 * Convenience function that calls the basic [sendAndAwait] function with parameters taken from the current event
	 * context.
	 *
	 * `null` will be provided to the callback if the modal times out before the user responds.
	 */
	public suspend fun <T, E : InteractionCreateEvent> sendAndAwait(
		context: EventContext<E>,
		callback: suspend (ModalSubmitInteraction?) -> T,
	): T {
		val interaction = context.event.interaction as? ModalParentInteractionBehavior
			?: error("Interaction ${context.event.interaction} does not support responding with a modal.")

		return sendAndAwait(context.getLocale(), interaction, callback)
	}

	/**
	 * Convenience function that calls the basic [sendAndAwait] function with parameters taken from the current command
	 * context.
	 *
	 * `null` will be provided to the callback if the modal times out before the user responds.
	 */
	public suspend fun <T> sendAndAwait(
		context: dev.kordex.core.commands.application.ApplicationCommandContext,
		callback: suspend (ModalSubmitInteraction?) -> T,
	): T {
		val interaction = context.genericEvent.interaction as? ModalParentInteractionBehavior
			?: error("Interaction ${context.genericEvent.interaction} does not support responding with a modal.")

		return sendAndAwait(context.getLocale(), interaction, callback)
	}

	/**
	 * Convenience function that calls the basic [sendAndAwait] function with parameters taken from the current
	 * component context.
	 *
	 * `null` will be provided to the callback if the modal times out before the user responds.
	 */
	public suspend fun <T> sendAndAwait(
		context: ComponentContext<*>,
		callback: suspend (ModalSubmitInteraction?) -> T,
	): T {
		val interaction = context.event.interaction as? ModalParentInteractionBehavior
			?: error("Interaction ${context.event.interaction} does not support responding with a modal.")

		return sendAndAwait(context.getLocale(), interaction, callback)
	}

	/**
	 * Convenience function that sends the modal, awaits its completion, and returns a deferred ephemeral interaction
	 * response.
	 *
	 * Returns `null` if the modal times out before the user responds.
	 */
	public suspend fun <E : InteractionCreateEvent> sendAndDeferEphemeral(
		context: EventContext<E>,
	): EphemeralMessageInteractionResponseBehavior? = sendAndAwait(context) {
		it?.deferEphemeralResponseUnsafe()
	}

	/**
	 * Convenience function that sends the modal, awaits its completion, and returns a deferred ephemeral interaction
	 * response.
	 *
	 * Returns `null` if the modal times out before the user responds.
	 */
	public suspend fun sendAndDeferEphemeral(
		context: dev.kordex.core.commands.application.ApplicationCommandContext,
	): EphemeralMessageInteractionResponseBehavior? = sendAndAwait(context) {
		it?.deferEphemeralResponseUnsafe()
	}

	/**
	 * Convenience function that sends the modal, awaits its completion, and returns a deferred ephemeral interaction
	 * response.
	 *
	 * Returns `null` if the modal times out before the user responds.
	 */
	public suspend fun sendAndDeferEphemeral(
		context: ComponentContext<*>,
	): EphemeralMessageInteractionResponseBehavior? = sendAndAwait(context) {
		it?.deferEphemeralResponseUnsafe()
	}

	/**
	 * Convenience function that sends the modal, awaits its completion, and returns a deferred public interaction
	 * response.
	 *
	 * Returns `null` if the modal times out before the user responds.
	 */
	public suspend fun <E : InteractionCreateEvent> sendAndDeferPublic(
		context: EventContext<E>,
	): PublicMessageInteractionResponseBehavior? = sendAndAwait(context) {
		it?.deferPublicResponseUnsafe()
	}

	/**
	 * Convenience function that sends the modal, awaits its completion, and returns a deferred public interaction
	 * response.
	 *
	 * Returns `null` if the modal times out before the user responds.
	 */
	public suspend fun sendAndDeferPublic(
		context: dev.kordex.core.commands.application.ApplicationCommandContext,
	): PublicMessageInteractionResponseBehavior? = sendAndAwait(context) {
		it?.deferPublicResponseUnsafe()
	}

	/**
	 * Convenience function that sends the modal, awaits its completion, and returns a deferred public interaction
	 * response.
	 *
	 * Returns `null` if the modal times out before the user responds.
	 */
	public suspend fun sendAndDeferPublic(
		context: ComponentContext<*>,
	): PublicMessageInteractionResponseBehavior? = sendAndAwait(context) {
		it?.deferPublicResponseUnsafe()
	}

	/**
	 * Converts a list of strings to list of snowflakes
	 *
	 * For some reason, Kord will always return a List of Strings for select menu values, despite most of them being
	 * lists of snowflakes. This function combats that safely and avoids unchecked cast warnings.
	 */
	private fun List<String>?.stringListToSnowflakeList(): List<Snowflake>? {
		this ?: return null

		val snowflakeList = mutableListOf<Snowflake>()

		this.forEach {
			snowflakeList.add(Snowflake(it))
		}

		return snowflakeList
	}
}
