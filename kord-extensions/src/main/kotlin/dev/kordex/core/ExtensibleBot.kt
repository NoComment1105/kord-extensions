/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:OptIn(PrivilegedIntent::class, KordPreview::class)

package dev.kordex.core

import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import dev.kord.core.behavior.requestMembers
import dev.kord.core.event.Event
import dev.kord.core.event.UnknownEvent
import dev.kord.core.event.gateway.DisconnectEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.interaction.*
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.gateway.handler.DefaultGatewayEventInterceptor
import dev.kord.core.on
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kordex.core.builders.ExtensibleBotBuilder
import dev.kordex.core.commands.application.ApplicationCommandRegistry
import dev.kordex.core.commands.chat.ChatCommandRegistry
import dev.kordex.core.components.ComponentRegistry
import dev.kordex.core.datacollection.DataCollector
import dev.kordex.core.events.EventHandler
import dev.kordex.core.events.KordExEvent
import dev.kordex.core.events.extra.GuildJoinRequestDeleteEvent
import dev.kordex.core.events.extra.GuildJoinRequestUpdateEvent
import dev.kordex.core.events.extra.models.GuildJoinRequestDelete
import dev.kordex.core.events.extra.models.GuildJoinRequestUpdate
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.impl.HelpExtension
import dev.kordex.core.extensions.impl.SentryExtension
import dev.kordex.core.koin.KordExContext
import dev.kordex.core.koin.KordExKoinComponent
import dev.kordex.core.types.Lockable
import dev.kordex.core.utils.MutableStringKeyedMap
import dev.kordex.core.utils.loadModule
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.koin.core.component.inject
import org.koin.dsl.bind
import java.util.concurrent.Executors
import kotlin.Throws
import kotlin.concurrent.thread

/**
 * An extensible bot, wrapping a Kord instance.
 *
 * This is your jumping-off point. ExtensibleBot provides a system for managing extensions, commands and event
 * handlers. Either subclass ExtensibleBot or use it as-is if it suits your needs.
 *
 * You shouldn't construct this class directly - use the builder pattern via the companion object's `invoke` method:
 * `ExtensibleBot(token) { extensions { add(::MyExtension) } }`.
 *
 * @param settings Bot builder object containing the bot's settings.
 * @param token Token for connecting to Discord.
 */
public open class ExtensibleBot(
	public val settings: ExtensibleBotBuilder,
	private val token: String,
) : KordExKoinComponent, Lockable {
	override var mutex: Mutex? = Mutex()
	override var locking: Boolean = settings.membersBuilder.lockMemberRequests

	protected var autoCompleteCoroutineThreads: Int = 0
	protected var interactionCoroutineThreads: Int = 0

	public val autoCompleteCoroutineContext: CoroutineDispatcher =
		Executors.newFixedThreadPool(settings.autoCompleteContextThreads) { r ->
			autoCompleteCoroutineThreads++
			Thread(r, "kordex-autocomplete-${autoCompleteCoroutineThreads - 1}")
		}.asCoroutineDispatcher()

	public val interactionCoroutineContext: CoroutineDispatcher =
		Executors.newFixedThreadPool(settings.interactionContextThreads) { r ->
			interactionCoroutineThreads++
			Thread(r, "kordex-interactions-${interactionCoroutineThreads - 1}")
		}.asCoroutineDispatcher()

	/** @suppress Meant for internal use by public inline function. **/
	public val kordRef: Kord by inject()

	/**
	 * A list of all registered event handlers.
	 */
	public open val eventHandlers: MutableList<EventHandler<out Event>> = mutableListOf()

	/**
	 * A map of the names of all loaded [Extension]s to their instances.
	 */
	public open val extensions: MutableStringKeyedMap<Extension> = mutableMapOf()

	/** @suppress **/
	public open val eventPublisher: MutableSharedFlow<Any> = MutableSharedFlow()

	/** A [Flow] representing a combined set of Kord events and Kord Extensions events. **/
	public open val events: SharedFlow<Any> = eventPublisher.asSharedFlow()

	/** @suppress **/
	public open var initialized: Boolean = false

	/** @suppress **/
	public open val logger: KLogger = KotlinLogging.logger {}

	/** @suppress **/
	public open val shutdownHook: Thread = thread(false) {
		runBlocking {
			close()
		}
	}

	private val dataCollector = DataCollector(settings.dataCollectionMode)

	/** @suppress Function that sets up the bot early on, called by the builder. **/
	public open suspend fun setup() {
		val kord = settings.kordBuilder(token) {
			cache {
				settings.cacheBuilder.builder.invoke(this, it)
			}

			defaultStrategy = settings.cacheBuilder.defaultStrategy

			if (settings.shardingBuilder != null) {
				sharding(settings.shardingBuilder!!)
			}

			enableShutdownHook = settings.hooksBuilder.kordShutdownHook

			settings.kordHooks.forEach { it() }

			gatewayEventInterceptor = DefaultGatewayEventInterceptor(
				customContextCreator = { _, _ ->
					mutableMapOf<String, Any>()
				}
			)
		}

		loadModule { single { kord } bind Kord::class }

		settings.cacheBuilder.dataCacheBuilder.invoke(kord, kord.cache)

		if (settings.kordEventFilter == null) {
			logger.debug { "Kord event filter predicate not set." }

			kord.on<Event> {
				sendKord(this@on)
			}
		} else {
			logger.debug { "Kord event filter predicate set, filtering Kord events." }

			kord.on<Event> {
				if (settings.kordEventFilter!!(this@on)) {
					sendKord(this@on)
				}
			}
		}

		addDefaultExtensions()
	}

	/** Start up the bot and log into Discord. **/
	public open suspend fun start() {
		settings.hooksBuilder.runBeforeStart(this)

		if (!initialized) registerListeners()

		@Suppress("TooGenericExceptionCaught")
		try {
			Runtime.getRuntime().addShutdownHook(shutdownHook)
		} catch (e: IllegalArgumentException) {
			logger.debug(e) { "Shutdown hook already added or thread is running." }
		} catch (e: Exception) {
			logger.warn(e) { "Unable to add shutdown hook." }
		}

		getKoin().get<Kord>().login {
			this.presence(settings.presenceBuilder)
			this.intents = Intents(settings.intentsBuilder!!)
		}
	}

	/**
	 * Stop the bot by logging out [Kord].
	 *
	 * This will leave extensions loaded and the Koin context intact, so later restarting of the bot is possible.
	 *
	 * @see close
	 **/
	public open suspend fun stop() {
		dataCollector.stop()
		interactionCoroutineContext.cancel()

		getKoin().get<Kord>().logout()
	}

	/**
	 * Stop the bot by unloading extensions, shutting down [Kord], and removing its Koin context.
	 *
	 * Restarting the bot after closing will result in undefined behaviour
	 * because the Koin context needed to start will no longer exist.
	 *
	 * You must fully rebuild closed bots to start again.
	 * Likewise, you must close previous bots before building a new one.
	 *
	 * @see stop
	 **/
	public open suspend fun close() {
		@Suppress("TooGenericExceptionCaught")
		try {
			Runtime.getRuntime().removeShutdownHook(shutdownHook)
		} catch (_: IllegalStateException) {
			logger.debug { "Shutdown in progress, unable to remove shutdown hook." }
		} catch (e: Exception) {
			logger.warn(e) { "Failed to remove shutdown hook." }
		}

		extensions.keys.forEach {
			unloadExtension(it)
		}

		dataCollector.stop()
		interactionCoroutineContext.cancel()

		getKoin().get<Kord>().shutdown()

		KordExContext.stopKoin()
	}

	/** Start up the bot and log into Discord, but launched via Kord's coroutine scope. **/
	public open fun startAsync(): Job =
		getKoin().get<Kord>().launch {
			start()
		}

	@Suppress("TooGenericExceptionCaught")
	public open suspend fun registerListeners() {
		val eventJson = Json {
			ignoreUnknownKeys = true
		}

		on<ReadyEvent> {
			try {
				dataCollector.start()
			} catch (e: Exception) {
				logger.warn(e) { "Exception thrown while setting up data collector" }
			}
		}

		on<GuildCreateEvent> {
			withLock {  // If configured, this won't be concurrent, saving larger bots from spammy rate limits
				try {
					if (
						settings.membersBuilder.guildsToFill == null ||
						settings.membersBuilder.guildsToFill!!.contains(guild.id)
					) {
						logger.debug { "Requesting members for guild: ${guild.name}" }

						guild.requestMembers {
							presences = settings.membersBuilder.fillPresences
							requestAllMembers()
						}.collect()
					}
				} catch (e: Exception) {
					logger.error(e) { "Exception thrown while requesting guild members" }
				}
			}
		}

		on<UnknownEvent> {
			try {
				val eventObj = when (name) {
					"GUILD_JOIN_REQUEST_DELETE" -> {
						val data: GuildJoinRequestDelete = eventJson.decodeFromJsonElement(this.data!!)

						GuildJoinRequestDeleteEvent(data)
					}

					"GUILD_JOIN_REQUEST_UPDATE" -> {
						val data: GuildJoinRequestUpdate = eventJson.decodeFromJsonElement(this.data!!)

						GuildJoinRequestUpdateEvent(data)
					}

					else -> return@on
				}

				send(eventObj)
			} catch (e: Exception) {
				logger.error(e) { "Failed to deserialize event: $data" }
			}
		}

		on<DisconnectEvent.DiscordCloseEvent> {
			logger.warn { "Disconnected: $closeCode" }
		}

		if (settings.chatCommandsBuilder.enabled) {
			on<MessageCreateEvent> {
				try {
					getKoin().get<ChatCommandRegistry>().handleEvent(this)
				} catch (e: Exception) {
					logger.error(e) { "Exception thrown while handling messsage creation event for chat commands" }
				}
			}
		} else {
			logger.debug {
				"Chat command support is disabled - set `enabled` to `true` in the `chatCommands` builder" +
					" if you want to use them."
			}
		}

		if (settings.applicationCommandsBuilder.enabled) {
			try {
				getKoin().get<ApplicationCommandRegistry>().initialRegistration()
			} catch (e: Exception) {
				logger.error(e) { "Exception thrown during initial interaction command registration phase" }
			}
		} else {
			logger.debug {
				"Application command support is disabled - set `enabled` to `true` in the " +
					"`applicationCommands` builder if you want to use them."
			}
		}

		if (!initialized) {
			eventHandlers.forEach { handler ->
				handler.listenerRegistrationCallable?.invoke() ?: logger.error {
					"Event handler $handler doesn't have a listener registration callback. This should never happen!"
				}
			}

			initialized = true
		}
	}

	/** This function adds all of the default extensions when the bot is being set up. **/
	public open suspend fun addDefaultExtensions() {
		val extBuilder = settings.extensionsBuilder

		if (extBuilder.helpExtensionBuilder.enableBundledExtension) {
			this.addExtension(::HelpExtension)
		}

		if (extBuilder.sentryExtensionBuilder.enable && extBuilder.sentryExtensionBuilder.feedbackExtension) {
			this.addExtension(::SentryExtension)
		}
	}

	/**
	 * Subscribe to an event. You shouldn't need to use this directly, but it's here just in case.
	 *
	 * You can subscribe to any type, realistically - but this is intended to be used only with Kord
	 * [Event] subclasses, and our own [KordExEvent]s.
	 *
	 * @param T Types of event to subscribe to.
	 * @param scope Coroutine scope to run the body of your callback under.
	 * @param consumer The callback to run when the event is fired.
	 */
	@Suppress("TooGenericExceptionCaught", "StringLiteralDuplication")
	public inline fun <reified T : Event> on(
		launch: Boolean = true,
		scope: CoroutineScope = kordRef,
		noinline consumer: suspend T.() -> Unit,
	): Job =
		events.buffer(Channel.UNLIMITED)
			.filterIsInstance<T>()
			.onEach {
				runCatching {
					if (launch) {
						scope.launch {
							try {
								consumer(it)
							} catch (t: Throwable) {
								logger.error(t) { "Exception thrown from low-level event handler: $consumer" }
							}
						}
					} else {
						consumer(it)
					}
				}.onFailure { logger.error(it) { "Exception thrown from low-level event handler: $consumer" } }
			}.catch { logger.error(it) { "Exception thrown from low-level event handler: $consumer" } }
			.launchIn(kordRef)

	/**
	 * @suppress Internal function used to additionally process all events. Don't call this yourself.
	 */
	@Suppress("TooGenericExceptionCaught")
	public suspend inline fun publishEvent(event: Event) {
		when (event) {
			// General interaction events
			is ButtonInteractionCreateEvent ->
				kordRef.launch(interactionCoroutineContext) {
					try {
						getKoin().get<ComponentRegistry>().handle(event)
					} catch (e: Exception) {
						logger.error(e) { "Exception thrown while handling button interaction event" }
					}
				}

			is SelectMenuInteractionCreateEvent ->
				kordRef.launch(interactionCoroutineContext) {
					try {
						getKoin().get<ComponentRegistry>().handle(event)
					} catch (e: Exception) {
						logger.error(e) { "Exception thrown while handling select menu interaction event" }
					}
				}

			is ModalSubmitInteractionCreateEvent ->
				kordRef.launch(interactionCoroutineContext) {
					try {
						getKoin().get<ComponentRegistry>().handle(event)
					} catch (e: Exception) {
						logger.error(e) { "Exception thrown while handling modal interaction event" }
					}
				}

			// Interaction command events
			is ChatInputCommandInteractionCreateEvent ->
				if (settings.applicationCommandsBuilder.enabled) {
					kordRef.launch(interactionCoroutineContext) {
						try {
							getKoin().get<ApplicationCommandRegistry>().handle(event)
						} catch (e: Exception) {
							logger.error(e) { "Exception thrown while handling slash command interaction event" }
						}
					}
				}

			is MessageCommandInteractionCreateEvent ->
				if (settings.applicationCommandsBuilder.enabled) {
					kordRef.launch(interactionCoroutineContext) {
						try {
							getKoin().get<ApplicationCommandRegistry>().handle(event)
						} catch (e: Exception) {
							logger.error(e) { "Exception thrown while handling message command interaction event" }
						}
					}
				}

			is UserCommandInteractionCreateEvent ->
				if (settings.applicationCommandsBuilder.enabled) {
					kordRef.launch(interactionCoroutineContext) {
						try {
							getKoin().get<ApplicationCommandRegistry>().handle(event)
						} catch (e: Exception) {
							logger.error(e) { "Exception thrown while handling user command interaction event" }
						}
					}
				}

			is AutoCompleteInteractionCreateEvent ->
				if (settings.applicationCommandsBuilder.enabled) {
					kordRef.launch(autoCompleteCoroutineContext) {
						try {
							getKoin().get<ApplicationCommandRegistry>().handle(event)
						} catch (e: Exception) {
							logger.error(e) { "Exception thrown while handling autocomplete interaction event" }
						}
					}
				}
		}

		eventPublisher.emit(event)
	}

	/**
	 * @suppress Internal function used to process Kord events. Don't call this yourself.
	 */
	protected suspend inline fun sendKord(event: Event) {
		publishEvent(event)
	}

	/**
	 * Submit an event, triggering any relevant event handlers.
	 *
	 * Events submitted using this function are filtered by [ExtensibleBotBuilder.kordEventFilter].
	 */
	public suspend inline fun send(event: Event, filter: Boolean = true) {
		if (!filter || settings.kordExEventFilter?.invoke(event) != false) {
			publishEvent(event)
		}
	}

	/**
	 * Install an [Extension] to this bot.
	 *
	 * This function will call the given builder function and store the resulting extension object, ready to be
	 * set up when the next [ReadyEvent] happens.
	 *
	 * @param builder Builder function (or extension constructor) that takes an [ExtensibleBot] instance and
	 * returns an [Extension].
	 */
	public open suspend fun addExtension(builder: () -> Extension) {
		val extensionObj = builder.invoke()

		if (extensions.contains(extensionObj.name)) {
			logger.error {
				"Extension with duplicate name ${extensionObj.name} loaded - unloading previously registered extension"
			}

			unloadExtension(extensionObj.name)
		}

		extensions[extensionObj.name] = extensionObj
		loadExtension(extensionObj.name)

		if (!extensionObj.loaded) {
			logger.warn { "Failed to set up extension: ${extensionObj.name}" }
		} else {
			logger.debug { "Loaded extension: ${extensionObj.name}" }

			settings.hooksBuilder.runExtensionAdded(this, extensionObj)
		}
	}

	/**
	 * Reload an unloaded [Extension] from this bot, by name.
	 *
	 * This function **does not** create a new extension object - it simply
	 * calls its `setup()` function. Loaded extensions can
	 * be unload again by calling [unloadExtension].
	 *
	 * This function simply returns if the extension isn't found.
	 *
	 * @param extension The name of the [Extension] to unload.
	 */
	public open suspend fun loadExtension(extension: String) {
		val extensionObj = extensions[extension] ?: return

		if (!extensionObj.loaded) {
			extensionObj.doSetup()
		}
	}

	/**
	 * Find the first loaded extension that is an instance of the type provided in `T`.
	 *
	 * This can be used to find an extension based on, for example, an implemented interface.
	 *
	 * @param T Types to match extensions against.
	 */
	public inline fun <reified T> findExtension(): T? =
		findExtensions<T>().firstOrNull()

	/**
	 * Find all loaded extensions that are instances of the type provided in `T`.
	 *
	 * This can be used to find extensions based on, for example, an implemented interface.
	 *
	 * @param T Types to match extensions against.
	 */
	public inline fun <reified T> findExtensions(): List<T> =
		extensions.values.filterIsInstance<T>()

	/**
	 * Unload an installed [Extension] from this bot, by name.
	 *
	 * This function **does not** remove the extension object - it simply
	 * removes its event handlers and commands. Unloaded extensions can
	 * be loaded again by calling [loadExtension].
	 *
	 * This function simply returns if the extension isn't found.
	 *
	 * @param extension The name of the [Extension] to unload.
	 */
	public open suspend fun unloadExtension(extension: String) {
		val extensionObj = extensions[extension] ?: return

		if (extensionObj.loaded) {
			extensionObj.doUnload()
		}
	}

	/**
	 * Remove an installed [Extension] from this bot, by name.
	 *
	 * This function will unload the given extension (if it's loaded), and remove the
	 * extension object from the list of registered extensions.
	 *
	 * @param extension The name of the [Extension] to unload.
	 *
	 * @suppress This is meant to be used with the module system, and isn't necessarily a user-facing API.
	 * You need to be quite careful with this!
	 */
	public open suspend fun removeExtension(extension: String) {
		unloadExtension(extension)

		extensions.remove(extension)
	}

	/**
	 * Directly register an [EventHandler] to this bot.
	 *
	 * Generally speaking, you shouldn't call this directly - instead, create an [Extension] and
	 * call the [Extension.event] function in your [Extension.setup] function.
	 *
	 * This function will throw an [EventHandlerRegistrationException] if the event handler has already been registered.
	 *
	 * @param handler The event handler to be registered.
	 * @throws EventHandlerRegistrationException Thrown if the event handler could not be registered.
	 */
	@Throws(EventHandlerRegistrationException::class)
	public inline fun <reified T : Event> addEventHandler(handler: EventHandler<T>) {
		if (eventHandlers.contains(handler)) {
			throw EventHandlerRegistrationException(
				"Event handler already registered in '${handler.extension.name}' extension."
			)
		}

		if (initialized) {
			handler.listenerRegistrationCallable?.invoke() ?: error(
				"Event handler $handler does not have a listener registration callback. This should never happen!"
			)
		}

		eventHandlers.add(handler)
	}

	/**
	 * Directly register an [EventHandler] to this bot.
	 *
	 * Generally speaking, you shouldn't call this directly - instead, create an [Extension] and
	 * call the [Extension.event] function in your [Extension.setup] function.
	 *
	 * This function will throw an [EventHandlerRegistrationException] if the event handler has already been registered.
	 *
	 * @param handler The event handler to be registered.
	 * @throws EventHandlerRegistrationException Thrown if the event handler could not be registered.
	 */
	@Suppress("TooGenericExceptionCaught")
	@Throws(EventHandlerRegistrationException::class)
	public inline fun <reified T : Event> registerListenerForHandler(handler: EventHandler<T>): Job {
		return on<T>(scope = handler.coroutineScope) {
			try {
				handler.call(this)
			} catch (e: Exception) {
				logger.error(e) {
					"Exception thrown by event handler in '${handler.extension.name}' extension - " +
						"Event type is ${T::class.simpleName} (from ${T::class.java.packageName}); handler is $handler"
				}
			}
		}
	}

	/**
	 * Directly remove a registered [EventHandler] from this bot.
	 *
	 * This function is used when extensions are unloaded, in order to clear out their event handlers.
	 * No exception is thrown if the event handler wasn't registered.
	 *
	 * @param handler The event handler to be removed.
	 */
	public open fun removeEventHandler(handler: EventHandler<out Event>): Boolean = eventHandlers.remove(handler)
}

/**
 * DSL function for creating a bot instance. This is the Kord Extensions entrypoint.
 *
 * `ExtensibleBot(token) { extensions { add(::MyExtension) } }`
 */
@Suppress("FunctionNaming")  // This is a factory function
public suspend fun ExtensibleBot(token: String, builder: suspend ExtensibleBotBuilder.() -> Unit): ExtensibleBot {
	val settings = ExtensibleBotBuilder()

	builder(settings)

	return settings.build(token)
}
