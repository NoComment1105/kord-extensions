import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
	repositories {
		maven {
			name = "Kord Snapshots"
			url = uri("https://repo.kord.dev/snapshots")
		}
	}
}

plugins {
	`maven-publish`

	kotlin("jvm")

	id("com.github.jakemarsden.git-hooks")
	id("org.jetbrains.dokka")
}

val projectVersion: String by project

group = "dev.kordex"
version = projectVersion

val printVersion = task("printVersion") {
	doLast {
		print(version.toString())
	}
}

repositories {
	// This is here because Dokka will fail to build in CI otherwise.

	google()
	mavenCentral()

	maven {
		name = "Kord Snapshots"
		url = uri("https://repo.kord.dev/snapshots")
	}
}

gitHooks {
	setHooks(mapOf("pre-commit" to "applyLicenses detekt"))
}

subprojects {
	group = "dev.kordex"
	version = projectVersion

	repositories {
		// This is here because Dokka will fail to build in CI otherwise.

		rootProject.repositories.forEach {
			if (it is MavenArtifactRepository) {
				maven {
					name = it.name
					url = it.url
				}
			}
		}
	}

	tasks.withType<KotlinCompile> {
		// Removing this block breaks the build, and I don't know why!
	}
}
