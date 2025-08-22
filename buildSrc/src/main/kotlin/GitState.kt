/*
 * Copyrighted (Kord Extensions, 2025). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

import kotlinx.serialization.Serializable

val DEFAULT_TRANSLATIONS_URL = "https://github.com/Kord-Extensions/translations.git"

@Serializable
class GitState(
	var translationsUrl: String = DEFAULT_TRANSLATIONS_URL,
)
