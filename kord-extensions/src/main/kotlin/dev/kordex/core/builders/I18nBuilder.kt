/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.builders

import dev.kord.common.asJavaLocale
import dev.kord.common.kLocale
import dev.kord.core.entity.interaction.Interaction
import dev.kordex.core.annotations.BotBuilderDSL
import dev.kordex.core.i18n.SupportedLocales
import dev.kordex.i18n.I18n
import dev.kordex.i18n.generated.CoreTranslations
import java.util.*
import dev.kord.common.Locale as KLocale

/** Builder used to configure i18n options. **/
@BotBuilderDSL
public class I18nBuilder {
	init {
		I18n.defaultLocale = SupportedLocales.ENGLISH
		I18n.defaultBundle = CoreTranslations.bundle
	}

	@get:Deprecated(
		"Use I18n.defaultLocale instead.",
		ReplaceWith("I18n.defaultLocale", "dev.kordex.i18n.I18n"),
		DeprecationLevel.WARNING
	)
	public var defaultLocale: Locale
		get() = I18n.defaultLocale
		set(value) { I18n.defaultLocale = value }

	/**
	 * List of [locales][KLocale] which are used for application command names (without [defaultLocale]).
	 */
	public var applicationCommandLocales: MutableList<KLocale> = mutableListOf()

	/**
	 * Callables used to resolve a Locale object for the given guild, channel, and user.
	 *
	 * Resolves to [defaultLocale] by default.
	 */
	public var localeResolvers: MutableList<LocaleResolver> = mutableListOf()

	/** Register a locale resolver, returning the required [Locale] object or `null`. **/
	public fun localeResolver(body: LocaleResolver) {
		localeResolvers.add(body)
	}

	/**
	 * Registers [locales] as application command languages.
	 *
	 * **Do not register [defaultLocale]!**
	 */
	@JvmName("applicationCommandLocale_v1")
	public fun applicationCommandLocale(
		vararg locales: KLocale,
	) {
		applicationCommandLocales.addAll(locales.toList())
	}

	/**
	 * Registers [locales] as application command languages.
	 *
	 * **Do not register [defaultLocale]!**
	 */
	@JvmName("applicationCommandLocale_v2")
	public fun applicationCommandLocale(
		vararg locales: Locale,
	) {
		applicationCommandLocales.addAll(locales.map { it.kLocale })
	}

	/**
	 * Registers [locales] as application command languages.
	 *
	 * **Do not register [defaultLocale]!**
	 */
	@JvmName("applicationCommandLocale_c1")
	public fun applicationCommandLocale(
		locales: Collection<KLocale>,
	) {
		applicationCommandLocales.addAll(locales)
	}

	/**
	 * Registers [locales] as application command languages.
	 *
	 * **Do not register [defaultLocale]!**
	 */
	@JvmName("applicationCommandLocale_c2")
	public fun applicationCommandLocale(
		locales: Collection<Locale>,
	) {
		applicationCommandLocales.addAll(locales.map { it.kLocale })
	}

	/**
	 * Registers a [LocaleResolver] using [Interaction.locale].
	 */
	public fun interactionUserLocaleResolver(): Unit =
		localeResolver { _, _, _, interaction ->
			interaction?.locale?.asJavaLocale()
		}

	/**
	 * Registers a [LocaleResolver] using [Interaction.guildLocale].
	 */
	public fun interactionGuildLocaleResolver(): Unit =
		localeResolver { _, _, _, interaction ->
			interaction?.guildLocale?.asJavaLocale()
		}
}
