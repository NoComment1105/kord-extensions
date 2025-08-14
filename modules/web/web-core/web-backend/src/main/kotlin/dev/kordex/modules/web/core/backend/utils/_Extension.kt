/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:OptIn(InternalAPI::class)

package dev.kordex.modules.web.core.backend.utils

import dev.kordex.core.ExtensibleBot
import dev.kordex.core.annotations.InternalAPI
import dev.kordex.core.extensions.Extension
import dev.kordex.core.utils.getOfOrPut
import dev.kordex.modules.web.core.backend.API_ROUTE_BASE_KEY
import dev.kordex.modules.web.core.backend.API_ROUTE_CALLBACKS_KEY
import dev.kordex.modules.web.core.backend.WebExtension
import dev.kordex.modules.web.core.backend.types.RouteCallback
import dev.kordex.modules.web.core.backend.types.RouteCallbackList
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.routing.*

private val logger = KotlinLogging.logger {}

public fun Extension.apiRoutes(callback: RouteCallback) {
	logger.debug { "Registering API route callback ($callback) for extension $name" }

	val webExtension = getKoin().get<ExtensibleBot>().findExtension<WebExtension>()!!

	extraData.getOfOrPut<RouteCallbackList>(API_ROUTE_CALLBACKS_KEY) {
		mutableListOf()
	}.add {
		route("/", callback)
	}

	extraData.getOfOrPut<Route>(API_ROUTE_BASE_KEY) {
		webExtension.server.configuredRoutes.extensionApiBase.route(name) {}
	}.apply {
		callback()
	}
}

// public fun EventContext<WebServerStartEvent>.websocket(path: String, body: WebsocketBuilderFun) {
// 	val socketBuilder = WebsocketBuilder(eventHandler.extension.name, body)
//
// 	if (!event.server.registries.websockets.add(path, socketBuilder)) {
// 		error("Websocket at $path for extension ${eventHandler.extension.name} already exists.")
// 	}
//
// 	// TODO: Replace?
// }
//
// public fun EventContext<WebServerStartEvent>.navigation(icon: Identifier, setup: ExtensionNavigation.() -> Unit) {
// 	val navigation = ExtensionNavigation(eventHandler.extension.name, icon, setup)
//
// 	navigation.setup()
//
// 	// TODO: Register!
// }
