plugins {
	`kordex-module`
	`published-module`

	kotlin("plugin.serialization")

	id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.kordex.modules"

metadata {
	name = "KordEx Web: Core Module"
	description = "KordEx module that provides a web interface, and a full set of APIs for working with it"
}

dokkaModule {
	moduleName = "Kord Extensions: Web Interface"
}

repositories {
	maven {
		name = "Kord Snapshots"
		url = uri("https://repo.kord.dev/snapshots")
	}
}

dependencies {
	detektPlugins(libs.detekt)
	detektPlugins(libs.detekt.libraries)

	implementation(libs.bundles.logging)
	implementation(libs.kotlin.stdlib)

	implementation(libs.ktor.logging)

	implementation(libs.bundles.ktor.server)

	implementation(project(":kord-extensions"))

	compileOnly(project(":modules:web:web-core:web-frontend"))
	shadow(project(":modules:web:web-core:web-frontend"))
}

tasks.shadowJar {
	this.configurations.clear()
	this.configurations.add(project.configurations.shadow.get())
}

tasks.build {
	finalizedBy(tasks.shadowJar)
}
