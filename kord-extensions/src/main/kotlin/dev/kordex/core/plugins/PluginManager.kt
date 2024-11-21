/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.plugins

import dev.kordex.core.KORDEX_VERSION
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.pf4j.*
import java.nio.file.Path

@Suppress("SpreadOperator")
/** Module manager, in charge of loading and managing module "plugins". **/
public open class PluginManager(
	roots: List<Path>,
	public var enabled: Boolean,
) : DefaultPluginManager(*roots.toTypedArray()) {
	protected val logger: KLogger = KotlinLogging.logger { }

	init {
		systemVersion = KORDEX_VERSION

		if ("-" in systemVersion) {
			systemVersion = systemVersion.split("-", limit = 2).first()
		}
	}

	override fun loadPlugins() {
		if (enabled) {
			super.loadPlugins()
		} else {
			logger.debug { "Plugin manager disabled, refusing to load plugins" }
		}
	}

	override fun loadPlugin(pluginPath: Path?): String? =
		if (enabled) {
			super.loadPlugin(pluginPath)
		} else {
			logger.debug { "Plugin manager disabled, refusing to load plugin at '$pluginPath'" }

			null
		}

	override fun createPluginDescriptorFinder(): PluginDescriptorFinder =
		PropertiesPluginDescriptorFinder()

	override fun createPluginLoader(): PluginLoader? {
		return CompoundPluginLoader()
			.add(DevelopmentPluginLoader(this)) { this.isDevelopment }
			.add(JarPluginLoader(this)) { this.isNotDevelopment }
			.add(DefaultPluginLoader(this)) { this.isNotDevelopment }
	}
}
