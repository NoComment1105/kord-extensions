import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
	repositories {
		maven {
			name = "Kord Snapshots"
			url = uri("https://repo.kordex.dev/mirror")
		}
	}
}

plugins {
	`maven-publish`

	kotlin("jvm")

	id("org.jetbrains.dokka")
	id("org.jetbrains.kotlinx.kover")
	id("org.sonarqube")

	`startup-plugin`
}

val projectVersion: String by project

group = "dev.kordex"
version = projectVersion

sonar {
	val org = "Kord-Extensions"
	val gitUrl = "https://github.com/${org}/kord-extensions/"
	val homepageUrl = "https://kordex.dev"

	properties {
		property("sonar.sourceEncoding", "UTF-8")
		property("sonar.projectName", "kord-extensions")
		property("sonar.projectKey", "${org}_${"kord-extensions"}")
		property("sonar.organization", "Kord-Extensions")
		property("sonar.projectVersion", rootProject.version.toString())
		property("sonar.host.url", System.getenv()["SONAR_HOST_URL"] ?: "")
		property("sonar.token", System.getenv()["SONAR_TOKEN"] ?: "" )
		property("sonar.scm.provider", "git")
		property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/report.xml")

		property("sonar.links.homepage", homepageUrl)
		property("sonar.links.ci", "$gitUrl/actions")
		property("sonar.links.scm", gitUrl)
		property("sonar.links.issue", "$gitUrl/issues")
	}
}

repositories {
	// This is here because Dokka and Kover will fail to build in CI otherwise.

	google()
	mavenCentral()

	maven {
		name = "Kord Snapshots"
		url = uri("https://repo.kordex.dev/mirror")
	}
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
