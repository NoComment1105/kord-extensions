/*
 * Copyrighted (Kord Extensions, 2025). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.modules.web.core.backend.utils

import dev.kordex.core.annotations.InternalAPI
import io.ktor.server.routing.*

@InternalAPI
public fun Route.deleteFromServer() {
	val node = this as? RoutingNode
		?: error("This isn't a routing node!")

	if (node.parent == null) {
		error("This node doesn't have a parent!")
	}

	(node.parent!!.children as MutableList<RoutingNode>)
		.remove(node)
}
