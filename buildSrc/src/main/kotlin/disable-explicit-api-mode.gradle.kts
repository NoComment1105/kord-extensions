import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
	kotlin("jvm")
}

kotlin {
	explicitApi = ExplicitApiMode.Disabled
}
