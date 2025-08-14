import okio.Path.Companion.toPath
import org.jetbrains.dokka.DokkaDefaults.moduleName
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files

buildscript {
	repositories {
		maven {
			name = "Kord Snapshots"
			url = uri("https://repo.kord.dev/snapshots")
		}
	}
}

plugins {
	`kordex-module`
	`published-module`
	`tested-module`
	`ksp-module`
}

getTranslations("core", "dev.kordex.core.i18n", "kordex.strings", "CoreTranslations")

metadata {
	name = "KordEx Core"
	description = "Core Kord Extensions module, providing everything you need to write a bot with KordEx"
}

dependencies {
	api(libs.icu4j)  // For translations
	api(libs.koin.core)
	api(libs.koin.logger)

	api(libs.data.collector.api)
	api(libs.kord)

	api(libs.bundles.logging) // Basic logging setup
	api(libs.jemoji)
	api(libs.kx.ser)
	api(libs.sentry)  // Needs to be transitive or bots will start breaking
	api(libs.toml)
	api(libs.pf4j)
	api(libs.oshi)

	api(project(":annotations:annotations"))
	api(project(":token-parser"))

	detektPlugins(libs.detekt)
	detektPlugins(libs.detekt.libraries)

	implementation(libs.bundles.commons)
	implementation(libs.kotlin.stdlib)

	testImplementation(libs.groovy)  // For logback config
	testImplementation(libs.jansi)
	testImplementation(libs.junit)
	testImplementation(libs.koin.test)
	testImplementation(libs.logback)
	testImplementation(libs.logback.groovy)

	ksp(project(":annotations:annotation-processor"))
	kspTest(project(":annotations:annotation-processor"))
}

val generateVersion = tasks.register("generateVersion") {
	group = "generation"
	description = "Generate KordEx metadata file"

	val output = layout.buildDirectory.file("generated/kordex/main/kotlin/dev/kordex/core/_Generated.kt")

	notCompatibleWithConfigurationCache("This task should always be run.")

	doLast {
		output.get().asFile.parentFile.mkdirs()
		output.get().asFile.writeText("""
package dev.kordex.core

/**
 * Gradle generated this file automatically.
 * It contains some build configuration data that KordEx uses at runtime.
 */

/** Current KordEx runtime version. **/
public const val KORDEX_VERSION: String = "${project.version}"

/** Current KordEx runtime version. **/
public const val BUILD_KORD_VERSION: String = "${libs.versions.kord.get()}"

/** Branch used to build this version of KordEx. **/
public const val KORDEX_GIT_BRANCH: String = "${getCurrentGitBranch()}"

/** Git hash corresponding with the commit used to build this version of KordEx. **/
public const val KORDEX_GIT_HASH: String = "${getCurrentGitHash()}"

		""".trimIndent())
	}
}

tasks.withType<KotlinCompile>()
	.configureEach { dependsOn(generateVersion) }

dokkaModule {
	moduleName = "Kord Extensions"
	includes.add("packages.md")
}
