/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:Suppress("TooGenericExceptionCaught")

package dev.kordex.core.commands.application

import dev.kord.common.entity.Snowflake
import dev.kord.core.event.interaction.AutoCompleteInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.MessageCommandInteractionCreateEvent
import dev.kord.core.event.interaction.UserCommandInteractionCreateEvent
import dev.kordex.core.commands.application.message.MessageCommand
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.commands.application.user.UserCommand
import dev.kordex.core.commands.converters.SlashCommandConverter
import dev.kordex.core.commands.getDefaultTranslatedDisplayName
import dev.kordex.core.registry.RegistryStorage
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

/**
 * [ApplicationCommandRegistry] which acts based off a specified storage interface.
 *
 * Discord lifecycles may not be implemented in this class and require manual updating.
 */
public open class StorageAwareApplicationCommandRegistry(
	builder: () -> RegistryStorage<Snowflake, ApplicationCommand<*>>,
) : ApplicationCommandRegistry() {

	protected open val commandRegistry: RegistryStorage<Snowflake, ApplicationCommand<*>> =
		builder.invoke()

	override suspend fun initialize(commands: List<ApplicationCommand<*>>) {
		commands.forEach { commandRegistry.register(it) }

		val registeredCommands = commandRegistry.entryFlow().toList()

		commands.forEach { command ->
			if (registeredCommands.none { it.hasCommand(command) }) {
				val commandId = createDiscordCommand(command)

				commandId?.let {
					commandRegistry.set(it, command)
				}
			}
		}
	}

	override suspend fun register(command: SlashCommand<*, *, *>): SlashCommand<*, *, *>? {
		val commandId = createDiscordCommand(command) ?: return null

		commandRegistry.set(commandId, command)

		return command
	}

	override suspend fun register(command: MessageCommand<*, *>): MessageCommand<*, *>? {
		val commandId = createDiscordCommand(command) ?: return null

		commandRegistry.set(commandId, command)

		return command
	}

	override suspend fun register(command: UserCommand<*, *>): UserCommand<*, *>? {
		val commandId = createDiscordCommand(command) ?: return null

		commandRegistry.set(commandId, command)

		return command
	}

	override suspend fun handle(event: ChatInputCommandInteractionCreateEvent) {
		val commandId = event.interaction.invokedCommandId
		val command = commandRegistry.get(commandId) as? SlashCommand<*, *, *>

		command ?: return logger.warn { "Received interaction for unknown slash command: $commandId" }

		command.doCall(event)
	}

	override suspend fun handle(event: MessageCommandInteractionCreateEvent) {
		val commandId = event.interaction.invokedCommandId
		val command = commandRegistry.get(commandId) as? MessageCommand<*, *>

		command ?: return logger.warn { "Received interaction for unknown message command: $commandId" }

		command.doCall(event)
	}

	override suspend fun handle(event: UserCommandInteractionCreateEvent) {
		val commandId = event.interaction.invokedCommandId
		val command = commandRegistry.get(commandId) as? UserCommand<*, *>

		command ?: return logger.warn { "Received interaction for unknown user command: $commandId" }

		command.doCall(event)
	}

	override suspend fun handle(event: AutoCompleteInteractionCreateEvent) {
		val commandId = event.interaction.command.rootId
		val command = commandRegistry.get(commandId) as? SlashCommand<*, *, *>

		command ?: return logger.warn { "Received autocomplete interaction for unknown command: $commandId" }

		if (command.arguments == null) {
			return logger.trace { "Command $command doesn't have any arguments." }
		}

		val option = event.interaction.command.options.filterValues { it.focused }.toList().firstOrNull()

		option ?: return logger.trace { "Autocomplete event for command $command doesn't have a focused option." }

		val arguments = command.cachedArguments!!

		val arg = arguments.args.firstOrNull {
			it.getDefaultTranslatedDisplayName() == option.first
		}

		arg ?: return logger.warn {
			"Autocomplete event for command $command has an unknown focused option: ${option.first}."
		}

		val callback = arg.converter.genericBuilder.autoCompleteCallback

		callback ?: return logger.trace {
			"Autocomplete event for command $command has an focused option without a callback: ${option.first}."
		}

		if (arguments.parseForAutocomplete) {
			val context = DummyAutocompleteCommandContext(command, event, command.name)

			for (priorArg in arguments.args) {
				if (priorArg == arg) {
					break
				}

				val argName = priorArg.getDefaultTranslatedDisplayName()
				val currentOption = event.interaction.command.options[argName]

				if (currentOption == null) {
					continue
				}

				try {
					(priorArg.converter as SlashCommandConverter).parseOption(context, currentOption)
				} catch (e: Exception) {
					logger.error(e) { "Failed to parse option $argName" }
				}
			}
		}

		callback(event.interaction, event)
	}

	override suspend fun unregister(command: SlashCommand<*, *, *>, delete: Boolean): SlashCommand<*, *, *>? =
		unregisterApplicationCommand(command, delete) as? SlashCommand<*, *, *>

	override suspend fun unregister(command: MessageCommand<*, *>, delete: Boolean): MessageCommand<*, *>? =
		unregisterApplicationCommand(command, delete) as? MessageCommand<*, *>

	override suspend fun unregister(command: UserCommand<*, *>, delete: Boolean): UserCommand<*, *>? =
		unregisterApplicationCommand(command, delete) as? UserCommand<*, *>

	protected open suspend fun unregisterApplicationCommand(
		command: ApplicationCommand<*>,
		delete: Boolean,
	): ApplicationCommand<*>? {
		val id = commandRegistry.constructUniqueIdentifier(command)

		val snowflake = commandRegistry.entryFlow()
			.firstOrNull { commandRegistry.constructUniqueIdentifier(it.value) == id }
			?.key

		snowflake?.let {
			if (delete) {
				deleteGeneric(command, it)
			}

			return commandRegistry.remove(it)
		}

		return null
	}

	protected open fun RegistryStorage.StorageEntry<Snowflake, ApplicationCommand<*>>.hasCommand(
		command: ApplicationCommand<*>,
	): Boolean {
		val key = commandRegistry.constructUniqueIdentifier(value)
		val other = commandRegistry.constructUniqueIdentifier(command)

		return key == other
	}
}
