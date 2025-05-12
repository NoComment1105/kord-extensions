/*
 * Copyrighted (Kord Extensions, 2025). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.modules.func.minecraft

import kotlinx.serialization.Serializable

@Serializable
data class PatchNoteEntries(
	val entries: List<PatchNoteEntry>,
	val version: Int,
)
