plugins {
	`kotlin-dsl`

	kotlin("plugin.serialization") version "2.3.10"
}

repositories {
	google()
	gradlePluginPortal()
	mavenCentral()

	maven("https://snapshots-repo.kordex.dev")
	maven("https://releases-repo.kordex.dev")
}

dependencies {
	implementation(kotlin("gradle-plugin", version = "2.3.10"))
	implementation(kotlin("serialization", version = "2.3.10"))

	implementation("com.github.ben-manes", "gradle-versions-plugin", "0.51.0")
	implementation("com.google.devtools.ksp", "com.google.devtools.ksp.gradle.plugin", "2.3.5")
	implementation("com.hanggrian", "kotlinpoet-dsl", "0.2")
	implementation("com.squareup", "kotlinpoet", "1.18.1")
	implementation("dev.kordex.gradle.i18n", "dev.kordex.gradle.i18n.gradle.plugin", "1.1.1")
	implementation("dev.yumi", "yumi-gradle-licenser", "1.2.0")
	implementation("io.gitlab.arturbosch.detekt", "detekt-gradle-plugin", "1.23.8")
	implementation("org.jetbrains.dokka", "dokka-gradle-plugin", "1.9.20")
	implementation("org.sonarqube:org.sonarqube.gradle.plugin:7.2.2.6593")
	implementation("org.jetbrains.kotlinx.kover:org.jetbrains.kotlinx.kover.gradle.plugin:0.9.1")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

	implementation(gradleApi())
	implementation(localGroovy())
}
