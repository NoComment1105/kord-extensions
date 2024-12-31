/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:OptIn(PrivilegedIntent::class)

package dev.kordex.core.builders

import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.ChannelBehavior
import dev.kord.core.builder.kord.KordBuilder
import dev.kord.core.entity.interaction.Interaction
import dev.kord.core.event.Event
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.NON_PRIVILEGED
import dev.kord.gateway.PrivilegedIntent
import dev.kord.gateway.builder.PresenceBuilder
import dev.kord.gateway.builder.Shards
import dev.kord.rest.builder.message.allowedMentions
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kordex.core.*
import dev.kordex.core.annotations.BotBuilderDSL
import dev.kordex.core.annotations.InternalAPI
import dev.kordex.core.commands.application.ApplicationCommandRegistry
import dev.kordex.core.commands.chat.ChatCommandRegistry
import dev.kordex.core.components.ComponentRegistry
import dev.kordex.core.extensions.impl.AboutExtension
import dev.kordex.core.i18n.TranslationsProvider
import dev.kordex.core.i18n.types.Key
import dev.kordex.core.koin.KordExContext
import dev.kordex.core.plugins.KordExPlugin
import dev.kordex.core.plugins.PluginManager
import dev.kordex.core.sentry.SentryAdapter
import dev.kordex.core.storage.DataAdapter
import dev.kordex.core.storage.toml.TomlDataAdapter
import dev.kordex.core.types.FailureReason
import dev.kordex.core.utils.getKoin
import dev.kordex.core.utils.loadModule
import dev.kordex.data.api.DataCollection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.logger.Level
import org.koin.dsl.bind
import org.koin.environmentProperties
import org.koin.fileProperties
import org.koin.logger.slf4jLogger
import java.io.File
import java.util.*

internal typealias LocaleResolver = suspend (
	guild: GuildBehavior?,
	channel: ChannelBehavior?,
	user: UserBehavior?,
	interaction: Interaction?,
) -> Locale?

internal typealias FailureResponseBuilder =
	suspend (MessageCreateBuilder).(message: Key, type: FailureReason<*>) -> Unit

/**
 * Builder class used for configuring and creating an [ExtensibleBot].
 *
 * This is a one-stop-shop for pretty much everything you could possibly need to change to configure your bot, via
 * properties and a bunch of DSL functions.
 */
@BotBuilderDSL
public open class ExtensibleBotBuilder {
	protected val logger: KLogger = KotlinLogging.logger {}

	/** Called to create an [ExtensibleBot], can be set to the constructor of your own subtype if needed. **/
	public var constructor: (ExtensibleBotBuilder, String) -> ExtensibleBot = ::ExtensibleBot

	/**
	 * The number of threads to use for interaction event coroutines.
	 *
	 * Defaults to double the available CPU cores, as returned by `Runtime.getRuntime().availableProcessors()`.
	 */
	public var interactionContextThreads: Int = Runtime.getRuntime().availableProcessors() * 2

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public val aboutBuilder: AboutBuilder = AboutBuilder()

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public val cacheBuilder: CacheBuilder = CacheBuilder()

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public val componentsBuilder: ComponentsBuilder = ComponentsBuilder()

	/**
	 * @suppress Builder that shouldn't be set directly by the user.
	 */
	public var dataAdapterCallback: () -> DataAdapter<*> = ::TomlDataAdapter

	/**
	 * @suppress Builder that shouldn't be set directly by the user.
	 */
	public var failureResponseBuilder: FailureResponseBuilder = { message, _ ->
		allowedMentions { }

		content = message.translate()
	}

	/**
	 * Whether the bot is running in development mode.
	 *
	 * By default, KordEx determines this by checking, in order:
	 * * For the `devMode` property.
	 * * For the `DEV_MODE` env var.
	 * * Whether the `ENVIRONMENT` env var is "dev" or "development".
	 *
	 * If none of these options work for you, you can set this property yourself.
	 */
	@OptIn(InternalAPI::class)
	public var devMode: Boolean = DEV_MODE

	/**
	 * Data collection mode, usually configured in other ways, but you may override that configuration here.
	 *
	 * Note: Changing this value at runtime won't do anything.
	 *
	 * By default, KordEx determines this by checking, in order:
	 * * The value of the `dataCollection` property.
	 * * The value of the `DATA_COLLECTION` env var.
	 * * The value of the `settings.dataCollection` property in your bot's `kordex.properties` file, typically
	 *   generated by the Kord Extensions Gradle plugin.
	 *
	 * The values checked above must be one of "none", "minimal", "standard" or "extra" to be valid, or KordEx will
	 * throw an exception.
	 *
	 * If all the above values are missing, this setting defaults to "standard".
	 *
	 * For more information on what data KordEx collects, how to get at it, and how it's stored, please see here:
	 * https://docs.kordex.dev/data-collection.html
	 */
	@OptIn(InternalAPI::class)
	public var dataCollectionMode: DataCollection = DATA_COLLECTION

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public var kordEventFilter: (suspend Event.() -> Boolean)? = null

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public var kordExEventFilter: (suspend Event.() -> Boolean)? = null

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public open val extensionsBuilder: ExtensionsBuilder = ExtensionsBuilder()

	/** @suppress Used for late execution of extensions builder calls, so plugins can be loaded first. **/
	protected open val deferredExtensionsBuilders: MutableList<suspend ExtensionsBuilder.() -> Unit> =
		mutableListOf()

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public val hooksBuilder: HooksBuilder = HooksBuilder()

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public val i18nBuilder: I18nBuilder = I18nBuilder()

	/** @suppress Plugin builder. **/
	public val pluginBuilder: PluginBuilder = PluginBuilder(this)

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public var intentsBuilder: (Intents.Builder.() -> Unit)? = {
		+Intents.NON_PRIVILEGED

		if (chatCommandsBuilder.enabled) {
			+Intent.MessageContent
		}

		getKoin().get<ExtensibleBot>().extensions.values.forEach { extension ->
			extension.intents.forEach {
				+it
			}
		}
	}

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public val membersBuilder: MembersBuilder = MembersBuilder()

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public val chatCommandsBuilder: ChatCommandsBuilder = ChatCommandsBuilder()

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public var presenceBuilder: PresenceBuilder.() -> Unit = { status = PresenceStatus.Online }

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public var shardingBuilder: ((recommended: Int) -> Shards)? = null

	/** @suppress Builder that shouldn't be set directly by the user. **/
	public val applicationCommandsBuilder: ApplicationCommandsBuilder = ApplicationCommandsBuilder()

	/** @suppress List of Kord builders, shouldn't be set directly by the user. **/
	public val kordHooks: MutableList<suspend KordBuilder.() -> Unit> = mutableListOf()

	/** @suppress Kord builder, creates a Kord instance. **/
	public var kordBuilder: suspend (token: String, builder: suspend KordBuilder.() -> Unit) -> Kord =
		{ token, builder ->
			Kord(token) { builder() }
		}

	/** Logging level Koin should use, defaulting to ERROR. **/
	public var koinLogLevel: Level = Level.ERROR

	/**
	 * Set an event-filtering predicate, which may selectively prevent Kord events from being processed by returning
	 * `false`.
	 *
	 * This only filters events created by Kord.
	 * For events submitted by Kord Extensions or loaded extensions, see [kordExEventFilter].
	 */
	@Deprecated(
		level = DeprecationLevel.ERROR,
		message = "Disambiguation: Renamed to kordEventFilter.",
		replaceWith = ReplaceWith("kordEventFilter"),
	)
	public fun eventFilter(predicate: suspend Event.() -> Boolean) {
		kordEventFilter = predicate
	}

	/**
	 * Set an event-filtering predicate, which may selectively prevent Kord-created events from being processed by
	 * returning `false`.
	 *
	 * This only filters events created by Kord.
	 * For events submitted by Kord Extensions or loaded extensions, see [kordExEventFilter].
	 */
	public fun kordEventFilter(predicate: suspend Event.() -> Boolean) {
		kordEventFilter = predicate
	}

	/**
	 * Set an event-filtering predicate, which may selectively prevent KordEx-created events from being processed by
	 * returning `false`.
	 *
	 * This only filters events submitted by Kord Extensions or loaded extensions.
	 * For events created by Kord, see [kordEventFilter].
	 */
	public fun kordExEventFilter(predicate: suspend Event.() -> Boolean) {
		kordExEventFilter = predicate
	}

	/**
	 * DSL function used to configure information about the bot.
	 *
	 * @see AboutBuilder
	 */
	@BotBuilderDSL
	public suspend fun about(builder: suspend AboutBuilder.() -> Unit) {
		builder(aboutBuilder)
	}

	/**
	 * DSL function used to configure the bot's caching options.
	 *
	 * @see CacheBuilder
	 */
	@BotBuilderDSL
	public suspend fun cache(builder: suspend CacheBuilder.() -> Unit) {
		builder(cacheBuilder)
	}

	/**
	 * Call this to register a custom data adapter class. Generally you'd pass a constructor here, but you can
	 * also provide a lambda if needed.
	 */
	@BotBuilderDSL
	public fun dataAdapter(builder: () -> DataAdapter<*>) {
		dataAdapterCallback = builder
	}

	/**
	 * DSL function used to configure the bot's plugin loading options.
	 *
	 * @see PluginBuilder
	 */
	@BotBuilderDSL
	public suspend fun plugins(builder: suspend PluginBuilder.() -> Unit) {
		builder(pluginBuilder)
	}

	/**
	 * DSL function used to configure the bot's components system.
	 *
	 * @see ComponentsBuilder
	 */
	@BotBuilderDSL
	public suspend fun components(builder: suspend ComponentsBuilder.() -> Unit) {
		builder(componentsBuilder)
	}

	/**
	 * Register the message builder responsible for formatting error responses, which are sent to users during command
	 * and component body execution.
	 */
	@BotBuilderDSL
	public fun errorResponse(builder: FailureResponseBuilder) {
		failureResponseBuilder = builder
	}

	/**
	 * DSL function used to insert code at various points in the bot's lifecycle.
	 *
	 * @see HooksBuilder
	 */
	@BotBuilderDSL
	public suspend fun hooks(builder: suspend HooksBuilder.() -> Unit) {
		builder(hooksBuilder)
	}

	/**
	 * DSL function allowing for additional Kord configuration builders to be specified, allowing for direct
	 * customisation of the Kord object.
	 *
	 * Multiple builders may be registered, and they'll be called in the order they were registered here. Builders are
	 * called after Kord Extensions has applied its own builder actions - so you can override the changes it makes here
	 * if they don't suit your bot.
	 *
	 * @see KordBuilder
	 */
	@BotBuilderDSL
	public fun kord(builder: suspend KordBuilder.() -> Unit) {
		kordHooks.add(builder)
	}

	/**
	 * Function allowing you to specify a callable that constructs and returns a Kord instance. This can be used
	 * to specify your own Kord subclass, if you need to - but shouldn't be a replacement for registering a [kord]
	 * configuration builder.
	 *
	 * @see Kord
	 */
	@BotBuilderDSL
	public fun customKordBuilder(builder: suspend (token: String, builder: suspend KordBuilder.() -> Unit) -> Kord) {
		kordBuilder = builder
	}

	/**
	 * DSL function used to configure the bot's chat command options.
	 *
	 * @see ChatCommandsBuilder
	 */
	@BotBuilderDSL
	public suspend fun chatCommands(builder: suspend ChatCommandsBuilder.() -> Unit) {
		builder(chatCommandsBuilder)
	}

	/**
	 * DSL function used to configure the bot's application command options.
	 *
	 * @see ApplicationCommandsBuilder
	 */
	@BotBuilderDSL
	public suspend fun applicationCommands(builder: suspend ApplicationCommandsBuilder.() -> Unit) {
		builder(applicationCommandsBuilder)
	}

	/**
	 * DSL function used to configure the bot's extension options, and add extensions. Calls to this function **do not
	 * run immediately**, so that plugins can be loaded beforehand.
	 *
	 * @see ExtensionsBuilder
	 */
	@BotBuilderDSL
	public open suspend fun extensions(builder: suspend ExtensionsBuilder.() -> Unit) {
		deferredExtensionsBuilders.add(builder)
	}

	/**
	 * DSL function used to configure the bot's intents.
	 *
	 * @param addDefaultIntents Whether to automatically add all non-privileged intents to the builder before running
	 * the given lambda.
	 * @param addDefaultIntents Whether to automatically add the required intents defined within each loaded extension
	 *
	 * @see Intents.Builder
	 */
	@BotBuilderDSL
	public fun intents(
		addDefaultIntents: Boolean = true,
		addExtensionIntents: Boolean = true,
		builder: Intents.Builder.() -> Unit,
	) {
		this.intentsBuilder = {
			if (addDefaultIntents) {
				+Intents.NON_PRIVILEGED

				if (chatCommandsBuilder.enabled) {
					+Intent.MessageContent
				}
			}

			if (addExtensionIntents) {
				getKoin().get<ExtensibleBot>().extensions.values.forEach { extension ->
					extension.intents.forEach {
						+it
					}
				}
			}

			builder()
		}
	}

	/**
	 * DSL function used to configure the bot's i18n settings.
	 *
	 * @see I18nBuilder
	 */
	@BotBuilderDSL
	public suspend fun i18n(builder: suspend I18nBuilder.() -> Unit) {
		builder(i18nBuilder)
	}

	/**
	 * DSL function used to configure the bot's member-related options.
	 *
	 * @see MembersBuilder
	 */
	@BotBuilderDSL
	public suspend fun members(builder: suspend MembersBuilder.() -> Unit) {
		builder(membersBuilder)
	}

	/**
	 * DSL function used to configure the bot's initial presence.
	 *
	 * @see PresenceBuilder
	 */
	@BotBuilderDSL
	public fun presence(builder: PresenceBuilder.() -> Unit) {
		this.presenceBuilder = builder
	}

	/**
	 * DSL function used to configure the bot's sharding settings.
	 *
	 * @see dev.kord.core.builder.kord.KordBuilder.shardsBuilder
	 */
	@BotBuilderDSL
	public fun sharding(shards: (recommended: Int) -> Shards) {
		this.shardingBuilder = shards
	}

	/** @suppress Internal function used to initially set up Koin. **/
	public open suspend fun setupKoin() {
		startKoinIfNeeded()

		hooksBuilder.runBeforeKoinSetup()

		addBotKoinModules()

		hooksBuilder.runAfterKoinSetup()
	}

	/** @suppress Creates a new KoinApplication if it has not already been started. **/
	@OptIn(KoinInternalApi::class)
	private fun startKoinIfNeeded() {
		var logLevel = koinLogLevel

		if (logLevel == Level.INFO || logLevel == Level.DEBUG) {
			// NOTE: Temporary workaround for Koin not supporting Kotlin 1.6
			logLevel = Level.ERROR
		}

		if (koinNotStarted()) {
			KordExContext.startKoin {
				slf4jLogger(logLevel)
				environmentProperties()

				if (File("koin.properties").exists()) {
					fileProperties("koin.properties")
				}
			}
		} else {
			getKoin().logger.level = logLevel
		}
	}

	/** @suppress Internal function that checks if Koin has been started. **/
	private fun koinNotStarted(): Boolean = KordExContext.getOrNull() == null

	/**
	 * @suppress Internal function that creates and loads the bot's main Koin modules.
	 * The modules provide important bot-related singletons.
	 **/
	private fun addBotKoinModules() {
		loadModule { single { this@ExtensibleBotBuilder } bind ExtensibleBotBuilder::class }
		loadModule { single { i18nBuilder.translationsProvider } bind TranslationsProvider::class }
		loadModule { single { chatCommandsBuilder.registryBuilder() } bind ChatCommandRegistry::class }
		loadModule { single { componentsBuilder.registryBuilder() } bind ComponentRegistry::class }

		loadModule {
			single {
				applicationCommandsBuilder.applicationCommandRegistryBuilder()
			} bind ApplicationCommandRegistry::class
		}

		loadModule {
			single {
				val adapter = extensionsBuilder.sentryExtensionBuilder.builder()

				if (extensionsBuilder.sentryExtensionBuilder.enable) {
					extensionsBuilder.sentryExtensionBuilder.setupCallback(adapter)
				}

				adapter
			} bind SentryAdapter::class
		}
	}

	/** @suppress Plugin-loading function. **/
	@Suppress("TooGenericExceptionCaught")
	public open suspend fun loadPlugins() {
		val manager = pluginBuilder.manager(pluginBuilder.pluginPaths, pluginBuilder.enabled)

		loadModule { single { manager } bind PluginManager::class }

		pluginBuilder.managerObj = manager

		if (!manager.enabled) {
			return
		}

		pluginBuilder.disabledPlugins.forEach(manager::disablePlugin)

		manager.loadPlugins()

		manager.plugins.forEach { wrapper ->
			try {
				val plugin = wrapper.plugin as? KordExPlugin

				plugin?.settingsCallbacks?.forEach { callback ->
					try {
						callback(this)
					} catch (e: Error) {
						logger.error(e) { "Error thrown while running settings callbacks for plugin: ${wrapper.pluginId}" }
					}
				}

				logger.info { "Loaded plugin: ${wrapper.pluginId} from ${wrapper.pluginPath}" }
			} catch (e: Error) {
				logger.error(e) { "Failed to load plugin: ${wrapper.pluginId} from ${wrapper.pluginPath}" }
			}
		}
	}

	/** @suppress Plugin-loading function. **/
	public open suspend fun startPlugins() {
		pluginBuilder.managerObj.startPlugins()
	}

	/** @suppress Internal function used to build a bot instance. **/
	public open suspend fun build(token: String): ExtensibleBot {
		logger.info {
			"Starting bot with Kord Extensions v$KORDEX_VERSION ($KORDEX_GIT_BRANCH@$KORDEX_GIT_HASH) " +
				"and Kord v$KORD_VERSION"
		}

		if (devMode) {
			logger.info {
				"Running in development mode - enabling development helpers."
			}

			kord {
				stackTraceRecovery = true
			}
		}

		hooksBuilder.beforeKoinSetup {  // We have to do this super-duper early for safety
			loadModule { single { dataAdapterCallback() } bind DataAdapter::class }
		}

		hooksBuilder.beforeKoinSetup {
			loadPlugins()

			deferredExtensionsBuilders.forEach { it(extensionsBuilder) }
		}

		setupKoin()

		val bot = constructor(this, token)

		loadModule { single { bot } bind ExtensibleBot::class }

		hooksBuilder.runCreated(bot)

		bot.setup()

		hooksBuilder.runSetup(bot)
		hooksBuilder.runBeforeExtensionsAdded(bot)

		bot.addExtension(::AboutExtension)

		@Suppress("TooGenericExceptionCaught")
		extensionsBuilder.extensions.forEach {
			try {
				bot.addExtension(it)
			} catch (e: Exception) {
				logger.error(e) {
					"Failed to set up extension: $it"
				}
			}
		}

		if (pluginBuilder.enabled) {
			startPlugins()
		}

		hooksBuilder.runAfterExtensionsAdded(bot)

		return bot
	}
}
