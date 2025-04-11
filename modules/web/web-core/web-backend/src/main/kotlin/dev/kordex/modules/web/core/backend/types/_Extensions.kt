/*
 * Copyrighted (Kord Extensions, 2025). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.modules.web.core.backend.types

import io.ktor.server.routing.Route

public typealias RouteCallback = Route.() -> Unit
public typealias RouteCallbackList = MutableList<RouteCallback>
