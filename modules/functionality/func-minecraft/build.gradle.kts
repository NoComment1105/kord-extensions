plugins {
	`kordex-module`
//	`published-module`
	`disable-explicit-api-mode`

	kotlin("plugin.serialization")
}

group = "dev.kordex.modules"

//metadata {
//	name = "KordEx Extra: Minecraft"
//	description = "KordEx extra module that provides Minecraf update notifications for bots"
//}

getTranslations(
	"func-minecraft",
	"dev.kordex.modules.func.minecraft.i18n",
	"kordex.func-minecraft",
	"MinecraftTranslations"
)

repositories {
	maven {
		name = "Kord Snapshots"
		url = uri("https://repo.kordex.dev/mirror")
	}
}

dependencies {
	detektPlugins(libs.detekt)
	detektPlugins(libs.detekt.libraries)

	implementation(libs.ksoup.entities)

	implementation(libs.bundles.logging)
	implementation(libs.kotlin.stdlib)
	implementation(libs.ktor.logging)

	implementation(project(":kord-extensions"))
}

dokkaModule {
	moduleName = "Kord Extensions: Minecraft Extension"
}
