/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:OptIn(InternalAPI::class)

package dev.kordex.modules.web.core.backend

import dev.kordex.core.annotations.InternalAPI
import dev.kordex.core.events.ExtensionStateEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ExtensionState
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.getOfOrNull
import dev.kordex.core.utils.getOfOrPut
import dev.kordex.modules.web.core.backend.config.WebServerConfig
import dev.kordex.modules.web.core.backend.server.WebServer
import dev.kordex.modules.web.core.backend.types.RouteCallback
import dev.kordex.modules.web.core.backend.types.RouteCallbackList
import dev.kordex.modules.web.core.backend.utils.deleteFromServer
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

public const val API_ROUTE_CALLBACKS_KEY: String = "extensions.web.apiRouteCallbacks"
public const val API_ROUTE_BASE_KEY: String = "extensions.web.apiRouteBase"

public class WebExtension(private val config: WebServerConfig) : Extension() {
	override val name: String = "kordex.web"

	private val logger = KotlinLogging.logger { }

	public var server: WebServer = WebServer(config)
		private set

	override suspend fun setup() {
		if (config.hostname == null) {
			logger.warn {
				"Hostname not configured - Authentication, CORS and frontend will be disabled."
			}
		}

		if (config.siteTitle == null) {
			logger.warn {
				"Site title not configured - Frontend will be disabled."
			}
		}

		server = WebServer(config)
		server.start()

		delay(2.seconds)

		event<ExtensionStateEvent> {
			check {
				failIf { event.extension == this@WebExtension }
			}

			action {
				when (event.state) {
					ExtensionState.LOADING -> event.extension.initialSetup()
					ExtensionState.UNLOADING -> event.extension.removeRoutes()
					ExtensionState.FAILED_LOADING -> event.extension.removeRoutes()

					else -> {}
				}
			}
		}
	}

	override suspend fun unload() {
		if (server.running) {
			server.stop()
		}
	}

	public fun Extension.initialSetup() {
		logger.trace { "Setting up extension: $name" }

		removeRoutes()

		extraData[API_ROUTE_BASE_KEY] = server.configuredRoutes.extensionApiBase.route(name) {}

		registerRoutes()
	}

	@Suppress("TooGenericExceptionCaught")
	public fun Extension.registerRoutes() {
		logger.trace { "Registering routes for extension: $name" }

		val callbacks = extraData.getOfOrPut<RouteCallbackList>(API_ROUTE_CALLBACKS_KEY) {
			mutableListOf<RouteCallback>()
		}

		val baseRoute = extraData.getOfOrPut<Route>(API_ROUTE_BASE_KEY) {
			server.configuredRoutes.extensionApiBase.route(name) {}
		}

		callbacks.forEach {
			try {
				it(baseRoute)
			} catch (e: Exception) {
				logger.error(e) { "Exception thrown while registering route for extension: $name -> $it" }
			}
		}
	}

	public fun Extension.removeRoutes() {
		logger.trace { "Removing routes for extension: $name" }

		extraData.getOfOrNull<Route>(API_ROUTE_BASE_KEY)?.deleteFromServer()
		extraData.remove(API_ROUTE_BASE_KEY)
	}
}
