/*
 * Copyrighted (Kord Extensions, 2025). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

val TRANSLATIONS_URL = System.getenv()["TRANSLATIONS_GIT_URL"]
	?: System.getProperties()["translationsGitUrl"]?.toString()
	?: DEFAULT_TRANSLATIONS_URL

val json = Json {
	prettyPrint = true
	encodeDefaults = true
}
val stateFileProvider = rootProject.layout.buildDirectory.dir("generated/git/state.json")

val gitDirProvider = rootProject.layout.buildDirectory.dir("generated/git")
val targetDirProvider = rootProject.layout.buildDirectory.dir("generated/git/translations")

@OptIn(ExperimentalSerializationApi::class)
fun getGitState(): GitState {
	val stateFile = stateFileProvider.get().asFile

	if (!stateFile.exists()) {
		setGitState(GitState(translationsUrl = TRANSLATIONS_URL))
	}

	return json.decodeFromStream(stateFile.inputStream())
}

@OptIn(ExperimentalSerializationApi::class)
fun setGitState(state: GitState) {
	val stateFile = stateFileProvider.get().asFile

	json.encodeToStream(state, stateFile.outputStream())
}

tasks.register("pullTranslations") {
	group = "generation"
	description = "Pull down translations from Git"

	outputs.dir(targetDirProvider)
	outputs.upToDateWhen { false }

	notCompatibleWithConfigurationCache("Need to check for updates every time to be safe.")

	doFirst {
		println("Using Git to update translation files. ")

		println(
			"You can provide a custom URL using the `translationsGitUrl` system property or the " +
				"`TRANSLATIONS_GIT_URL` env var."
		)

		println()

		val gitDir = gitDirProvider.get().asFile
		val targetDir = targetDirProvider.get().asFile

		if (!gitDir.exists()) {
			println("Creating: ${gitDir.absolutePath}")

			gitDir.mkdirs()
		}

		var state = getGitState()

		if (state.translationsUrl != TRANSLATIONS_URL) {
			println("Translations Git URL has changed - deleting old files at: ${targetDir.absolutePath}")
			println("  Old: ${state.translationsUrl}")
			println("  Current: $TRANSLATIONS_URL")

			state.translationsUrl = TRANSLATIONS_URL

			setGitState(state)

			if (targetDir.exists()) {
				targetDir.deleteRecursively()
			}
		}

		if (!targetDir.resolve(".git").exists()) {
			runCommand(
				"git clone $TRANSLATIONS_URL translations",
				gitDir
			)
		} else {
			runCommand(
				"git pull",
				targetDir
			)
		}
	}
}
