/*
 * Copyrighted (Kord Extensions, 2025). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

import dev.kordex.gradle.plugins.i18n.I18nExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

fun Project.getTranslations(
	name: String,
	classesPackage: String,
	bundle: String = name,
	translationsClass: String = "Translations",
) {
	val gitDir = rootProject.layout.buildDirectory.dir("generated/git/translations")
	val outputDir = project.layout.buildDirectory.dir("translations")

	val copyTask = tasks.register<Sync>("copyTranslations") {
		group = "generation"
		description = "Copy correct module translations."

		from(gitDir.get().dir(name))
		into(outputDir.get().dir("kordex"))

		dependsOn(rootProject.tasks.named("pullTranslations"))
	}

	with(extensions.getByType<I18nExtension>()) {
		bundle(bundle, "$classesPackage.generated") {
			className = translationsClass
			basePath = outputDir.get().asFile
		}
	}

	extensions
		.getByType<SourceSetContainer>()
		.first { it.name == "main" }
		.output.dir(
			mapOf("builtBy" to copyTask),
			outputDir
		)

	afterEvaluate {
		tasks.named("generateTranslationsClass") {
			dependsOn(copyTask)
		}

		tasks.named("classes") {
			dependsOn(tasks.named("generateTranslationsClass"))
		}
	}
}
