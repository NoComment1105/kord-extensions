/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.modules.func.mappings.exceptions

/**
 * Thrown when an unsupported namespace is configured.
 *
 * @property namespace The invalid namespace.
 **/
class UnsupportedNamespaceException(val namespace: String) : Exception(
	"Unknown/unsupported namespace: $namespace"
)
