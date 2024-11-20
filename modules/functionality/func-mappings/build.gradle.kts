plugins {
	`kordex-module`
	`published-module`
	`disable-explicit-api-mode`
	`ksp-module`
}

getTranslations(
	"func-mappings",
	"dev.kordex.modules.func.mappings.i18n",
	"kordex.func-mappings",
	"MappingsTranslations"
)

metadata {
	name = "KordEx Extra: Mappings"
	description = "KordEx extra module that provides Minecraft mappings functionality for bots"
}

repositories {
	maven {
		name = "Kord Snapshots"
		url = uri("https://repo.kord.dev/snapshots")
	}

	maven {
		name = "FabricMC"
		url = uri("https://maven.fabricmc.net/")
	}

	maven {
		name = "QuiltMC (Releases)"
		url = uri("https://maven.quiltmc.org/repository/release/")
	}

	maven {
		name = "QuiltMC (Snapshots)"
		url = uri("https://maven.quiltmc.org/repository/snapshot/")
	}

	maven {
		name = "Shedaniel"
		url = uri("https://maven.shedaniel.me")
	}

	maven {
		name = "JitPack"
		url = uri("https://jitpack.io")
	}
}

dependencies {
	api(libs.linkie) {
		exclude("ch.qos.logback", "logback-classic")
	}

	detektPlugins(libs.detekt)
	detektPlugins(libs.detekt.libraries)

	implementation(libs.bundles.logging)
	implementation(libs.kotlin.stdlib)

	testImplementation(libs.groovy)  // For logback config
	testImplementation(libs.jansi)
	testImplementation(libs.logback)
	testImplementation(libs.logback.groovy)

	implementation(project(":kord-extensions"))

	implementation(project(":annotations:annotations"))
	ksp(project(":annotations:annotation-processor"))
}

group = "dev.kordex.modules"

dokkaModule {
	moduleName = "Kord Extensions: Mappings Extension"
}
