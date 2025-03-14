/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.modules.web.core.backend

import dev.kordex.core.extensions.Extension
import dev.kordex.modules.web.core.backend.config.WebServerConfig
import dev.kordex.modules.web.core.backend.server.WebServer

public class WebExtension(private val config: WebServerConfig) : Extension() {
	override val name: String = "kordex.web"

	public var server: WebServer = WebServer(config)
		private set

	override suspend fun setup() {
		server = WebServer(config)

		server.start()
	}

	override suspend fun unload() {
		if (server.running) {
			server.stop()
		}
	}
}
