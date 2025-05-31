/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core

import dev.kordex.core.annotations.InternalAPI
import dev.kordex.core.builders.ExtensibleBotBuilder
import dev.kordex.core.utils.envOrNull
import dev.kordex.data.api.DataCollection
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger("dev.kordex.core._Properties")

private fun loadResource(resource: String): Properties? {
	val stream = ExtensibleBotBuilder::class.java.getResourceAsStream(resource)

	if (stream == null) {
		logger.warn { "Unable to load resource file: $resource" }

		return null
	}

	val props = Properties()

	props.load(stream)

	return props
}

/** Convenient access to the properties stored within `kordex.properties` in your bot's resources. **/
public val kordexProps: Properties? by lazy { loadResource("/kordex.properties") }

/** Convenient access to the properties stored within `kordex-build.properties` in your bot's resources. **/
public val kordexBuildProps: Properties? by lazy { loadResource("/kordex-build.properties") }

/**
 * Location of the data collection state file.
 *
 * Don't delete this, otherwise KordEx can't automatically remove your data when you disable data collection.
 */
public val COLLECTION_STATE_LOCATION: String by lazy {
	System.getProperties()["dataCollectionState"] as? String
		?: envOrNull("DATA_COLLECTION_STATE")
		?: "./data/data-collection.properties"
}

/**
 * Data collection UUID, if you need to specify one instead of having the storage system take care of it.
 *
 * Must be a valid UUID.
 */
public val DATA_COLLECTION_UUID: UUID? by lazy {
	(
		System.getProperties()["dataCollectionUUID"] as? String
			?: envOrNull("DATA_COLLECTION_UUID")
		)?.let { UUID.fromString(it) }
}

/**
 * Data collection setting, defaulting to Standard if not set.
 *
 * Don't check this directly – use the `dataCollectionMode` property in `ExtensibleBotBuilder` instead!
 */
@InternalAPI
public val DATA_COLLECTION: DataCollection by lazy {
	val value = System.getProperties()["dataCollection"] as? String
		?: envOrNull("DATA_COLLECTION")
		?: kordexProps?.get("settings.dataCollection") as? String
		?: DataCollection.Standard.readable

	DataCollection.fromDB(value)
}

/**
 * Bot version, as provided by the bot's `kordex.properties` resource.
 *
 * Don't check this directly — use the `botVersion` property in `ExtensibleBotBuilder` instead!
 */
@InternalAPI
public val BOT_VERSION: String? by lazy {
	kordexProps?.get("versions.bot") as? String
}

/**
 * Dev-mode configuration based on properties and env vars.
 *
 * Don't check this directly – use the `devMode` property in `ExtensibleBotBuilder` instead!
 */
@InternalAPI
public val DEV_MODE: Boolean =
	System.getProperty("devMode").toBoolean() ||
		envOrNull("DEV_MODE") != null ||
		envOrNull("ENVIRONMENT") in arrayOf("dev", "development")

/** Configured first-party KordEx modules. **/
public val KORDEX_MODULES: List<String> by lazy {
	val modules = kordexProps?.get("modules") as? String

	modules?.split(", ")
		?: emptyList()
}

/** Current Kord Extensions version. **/
public val KORDEX_VERSION: String? by lazy {
	kordexProps?.get("versions.kordEx") as? String
		?: kordexBuildProps?.get("versions.kordEx") as? String
}

/** Current Kord version. **/
public val KORD_VERSION: String? by lazy {
	kordexProps?.get("versions.kord") as? String
		?: kordexProps?.get("kordVersion") as? String
		?: kordexBuildProps?.get("versions.kord") as? String
}

/** Git branch used to build this KordEx release. **/
public val KORDEX_GIT_BRANCH: String? by lazy {
	kordexBuildProps?.get("git.branch") as? String
}

/** Hash corresponding with the Git commit used to build this KordEx release. **/
public val KORDEX_GIT_HASH: String? by lazy {
	kordexBuildProps?.get("git.hash") as? String
}
