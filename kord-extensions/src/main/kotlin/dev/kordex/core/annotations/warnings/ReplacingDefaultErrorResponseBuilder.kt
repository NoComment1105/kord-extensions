/*
 * Copyrighted (Kord Extensions, 2025). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.annotations.warnings

@RequiresOptIn(
	message = "This function replaces the default error response builder. Make sure you always provide some content " +
		"and never leave an empty message, or your bot may throw cryptic errors in some situations.",
	level = RequiresOptIn.Level.WARNING
)
@Target(AnnotationTarget.FUNCTION)
public annotation class ReplacingDefaultErrorResponseBuilder
