/*
 * Copyrighted (Kord Extensions, 2025). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:Suppress("MagicNumber")

package dev.kordex.modules.func.minecraft

import com.mohamedrejeb.ksoup.entities.KsoupEntities
import dev.kord.common.annotation.KordPreview
import dev.kord.common.asJavaLocale
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.builder.components.emoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.TopGuildMessageChannel
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.addFile
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_FUCHSIA
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.checks.isBotAdmin
import dev.kordex.core.checks.isBotOwner
import dev.kordex.core.checks.or
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.pagination.pages.Page
import dev.kordex.core.utils.scheduling.Scheduler
import dev.kordex.core.utils.scheduling.Task
import dev.kordex.core.utils.toReaction
import dev.kordex.modules.func.minecraft.i18n.generated.MinecraftTranslations
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createTempFile
import kotlin.io.path.writeBytes
import kotlin.time.Clock

private const val PAGINATOR_TIMEOUT = 60_000L  // One minute
private const val CHUNK_SIZE = 10

private const val BASE_URL = "https://launchercontent.mojang.com/v2"
private const val JSON_URL = "$BASE_URL/javaPatchNotes.json"

private const val CHECK_DELAY = 60L

private val IMG_REGEX = "<img([^>]+)>".toRegex()
private val IMG_ALT_REGEX = "alt=\"(?<alt>[^\"]+)\"".toRegex()
private val IMG_SRC_REGEX = "src=\"?(?<url>[^\"\\s>]+)\"?".toRegex()
private val LINK_REGEX = "<a href=\"?(?<url>[^\"]+)\"?[^>]*>(?<text>[^<]+)</a>".toRegex()

@Suppress("MagicNumber", "UnderscoresInNumericLiterals")
private val CHANNELS: List<Snowflake> = listOf(
	Snowflake(838805249271267398L),  // Community
)

// TODO: Configuration Builder
// TODO: Figure out storage
// TODO: Separate admin command for bot admins/owner on configurable guild
// TODO: Patch notes URL with basic templating
// TODO: Configurable ping role
// TODO: Configurable threading and publishing

class MinecraftExtension : Extension() {
	override val name: String = "minecraft"

	private val logger = KotlinLogging.logger { }

	private val client = HttpClient {
		install(ContentNegotiation) {
			json(
				Json {
					ignoreUnknownKeys = true
				}
			)
		}

		expectSuccess = true
	}

	private val scheduler = Scheduler()

	private var checkTask: Task? = null
	private var knownVersions: MutableSet<String> = mutableSetOf()
	private lateinit var currentEntries: PatchNoteEntries

	@OptIn(KordPreview::class)
	override suspend fun setup() {
		populateVersions()

		checkTask = scheduler.schedule(CHECK_DELAY, callback = ::checkTask)

		ephemeralSlashCommand { //  /mc
			name = MinecraftTranslations.Command.Mc.name
			description = MinecraftTranslations.Command.Mc.description

			allowInDms = false

			ephemeralSubCommand(::CheckArguments) {
				name = MinecraftTranslations.Command.Mc.Get.name
				description = MinecraftTranslations.Command.Mc.Get.description

				action {
					val locale = getLocale()

					if (!::currentEntries.isInitialized) {
						respond {
							content = MinecraftTranslations.Errors.notReady
								.translateLocale(locale)
						}

						return@action
					}

					val patch = if (arguments.version == null) {
						currentEntries.entries.first()
					} else {
						currentEntries.entries.firstOrNull { it.version.equals(arguments.version, true) }
					}

					if (patch == null) {
						respond {
							content = MinecraftTranslations.Errors.unknownMcVersion
								.withLocale(locale)
								.translateNamed(
									"version" to arguments.version
								)
						}

						return@action
					}

					respond {
						patchNotes(patch.get(), locale = locale)
					}
				}
			}

			ephemeralSubCommand {
				name = MinecraftTranslations.Command.Mc.Versions.name
				description = MinecraftTranslations.Command.Mc.Versions.description

				action {
					val locale = getLocale()

					if (!::currentEntries.isInitialized) {
						respond {
							content = MinecraftTranslations.Errors.notReady
								.translateLocale(locale)
						}

						return@action
					}

					editingPaginator {
						timeoutSeconds = PAGINATOR_TIMEOUT

						knownVersions.chunked(CHUNK_SIZE).forEach { chunk ->
							page(
								Page {
									title = MinecraftTranslations.Command.Mc.Versions.Embed.header
										.translateLocale(locale)

									color = DISCORD_FUCHSIA

									description = chunk.joinToString("\n") {
										MinecraftTranslations.Command.Mc.Versions.Embed.listItem
											.withLocale(locale)
											.translateNamed(
												"item" to it
											)
									}

									footer {
										text = MinecraftTranslations.Command.Mc.Versions.Embed.footer
											.withLocale(locale)
											.translateNamed(
												"number" to currentEntries.entries.size
											)
									}
								}
							)
						}
					}.send()
				}
			}
		}

		ephemeralSlashCommand { // /mc-admin
			name = MinecraftTranslations.Command.McAdmin.name
			description = MinecraftTranslations.Command.McAdmin.description

			allowInDms = false

			guild(null)  // TODO

			check {
				isBotAdmin()
				or { isBotOwner() }
			}

			ephemeralSubCommand(::CheckArguments) {
				name = MinecraftTranslations.Command.McAdmin.Forget.name
				description = MinecraftTranslations.Command.McAdmin.Forget.description

				action {
					val locale = getLocale()

					if (!::currentEntries.isInitialized) {
						respond {
							content = MinecraftTranslations.Errors.notReady
								.translateLocale(locale)
						}

						return@action
					}

					val version = if (arguments.version == null) {
						currentEntries.entries.first().version
					} else {
						currentEntries.entries.firstOrNull {
							it.version.equals(arguments.version, true)
						}?.version
					}

					if (version == null) {
						respond {
							content = MinecraftTranslations.Errors.unknownMcVersion
								.withLocale(locale)
								.translateNamed(
									"version" to arguments.version
								)
						}

						return@action
					}

					knownVersions.remove(version)

					respond { content = "Version forgotten: `$version`" }
				}
			}

			ephemeralSubCommand {
				name = MinecraftTranslations.Command.McAdmin.Run.name
				description = MinecraftTranslations.Command.McAdmin.Run.description

				action {
					respond {
						content = MinecraftTranslations.Command.McAdmin.Run.response
							.translateLocale(getLocale())
					}

					checkTask?.callNow()
				}
			}
		}
	}

	suspend fun populateVersions() {
		currentEntries = client.get(JSON_URL).body()

		currentEntries.entries.forEach { knownVersions.add(it.version) }
	}

	@Suppress("TooGenericExceptionCaught")
	suspend fun checkTask() {
		try {
			val now = Clock.System.now()

			currentEntries = client.get(JSON_URL + "?cbt=${now.epochSeconds}").body()

			currentEntries.entries.forEach {
				if (it.version !in knownVersions) {
					relayUpdate(it.get())
					knownVersions.add(it.version)
				}
			}
		} catch (t: Throwable) {
			logger.error(t) { "Check task run failed" }
		} finally {
			checkTask = scheduler.schedule(CHECK_DELAY, callback = ::checkTask)
		}
	}

	@Suppress("TooGenericExceptionCaught")
	suspend fun relayUpdate(patchNote: PatchNote) =
		CHANNELS.mapNotNull {
			try {
				kord.getChannelOf<TopGuildMessageChannel>(it)
			} catch (t: Throwable) {
				logger.warn(t) { "Unable to get channel of ID: ${it.value}" }

				null
			}
		}.forEach { it.relay(patchNote) }

	suspend fun String.formatHTML(): Markdown {
		var result = this

		result = result.replace("\u200B", "")
		result = result.replace("<p></p>", "")

		result = result.replace("<hr/?>".toRegex(), "\\_\\_\\_\\_\\_\\_\\_\\_\\_\\_\\_\\_")
		result = result.replace("</hr>", "")

		result = result.replace("\n*</p>\n+<p>\n*".toRegex(), "\n\n")
		result = result.replace("\n*</*p>\n*".toRegex(), "\n")

		result = result.replace("<strong>", "**")
		result = result.replace("</strong>", "**")

		result = result.replace("<code>", "`")
		result = result.replace("</code>", "`")

		@Suppress("MagicNumber")
		for (i in 1..6) {
			result = result.replace("\n*<h$i>\n*".toRegex(), "\n\n${"#".repeat(i)} ")
			result = result.replace("\n*</h$i>\n*".toRegex(), "\n")
		}

		result = result.replace("\n*<[ou]l>\n*".toRegex(), "\n\n")
		result = result.replace("\n*</[ou]l>\n*".toRegex(), "\n\n")

		result = result.replace("\n*</li>\n+<li>\n*".toRegex(), "\n- ")
		result = result.replace("(\n{2,})?<li>\n*".toRegex(), "\n- ")
		result = result.replace("\n*</li>\n*".toRegex(), "\n\n")

		val links = LINK_REGEX.findAll(result)

		links.forEach {
			result = result.replace(
				it.value,
				"[${it.groups["text"]?.value}](${it.groups["url"]?.value})"
			)
		}

		val images = IMG_REGEX.findAll(result)

		val imageList = images.mapNotNull {
			val alt = IMG_ALT_REGEX.find(it.value)
				?.groups["alt"]?.value

			val url = IMG_SRC_REGEX.find(it.value)
				?.groups["url"]?.value

			result = result.replace(it.value, "")

			if (alt == null || url == null) {
				null
			} else {
				Image(url, alt)
			}
		}

		imageList.forEachIndexed { index, image ->
			val extension = image.url.split(".").last()

			@Suppress("TooGenericExceptionCaught")
			try {
				image.path = createTempFile("image-$index", extension)
				image.path.writeBytes(client.get(image.url).bodyAsBytes())
				image.path.toFile().deleteOnExit()
			} catch (e: Exception) {
				logger.error(e) { "Failed to download image: ${image.url}" }
			}
		}

		return Markdown(
			body = KsoupEntities.decodeHtml4(result.trim('\n')),
			images = imageList.toList()
		)
	}

	fun String.truncateMarkdown(maxLength: Int = 4096): Pair<String, Int> {
		var result = this

		if (length > maxLength) {
			val truncated = result.substring(0, maxLength).substringBeforeLast("\n")
			val remaining = result.substringAfter(truncated).count { it == '\n' }

			result = truncated

			return result to remaining
		}

		return result to 0
	}

	private suspend fun MessageBuilder.patchNotes(patchNote: PatchNote, maxLength: Int = 4000, locale: Locale) {
		val markdown = patchNote.body.formatHTML()
		val (truncated, remaining) = markdown.body.truncateMarkdown(maxLength)

		actionRow {
			linkButton("https://quiltmc.org/mc-patchnotes/#${patchNote.version}") {
				label = MinecraftTranslations.Buttons.ReadMore.text
					.translateLocale(locale)

				emoji("🔗".toReaction() as ReactionEmoji.Unicode)
			}
		}

		markdown.images.take(4).forEach { image -> addFile(image.path) }

		embed {
			title = patchNote.title
			color = DISCORD_GREEN

			description = truncated

			if (remaining > 0) {
				description += "\n\n" +
					MinecraftTranslations.Embed.truncated
						.withLocale(locale)
						.translateNamed("lines" to remaining)
			}

			thumbnail {
				url = "$BASE_URL${patchNote.image.url}"
			}

			footer {
				text = "https://quiltmc.org/mc-patchnotes/#${patchNote.version}"
			}
		}
	}

	private suspend fun TopGuildMessageChannel.relay(patchNote: PatchNote, maxLength: Int = 1000) {
		val message = createMessage {
// 			if (guildId == COMMUNITY_GUILD) {
// 				content = "<@&$MINECRAFT_UPDATE_PING_ROLE>"
// 			}

			patchNotes(patchNote, maxLength, guild.asGuild().preferredLocale.asJavaLocale())
		}

		val title = if (patchNote.title.startsWith("minecraft ", true)) {
			patchNote.title.split(" ", limit = 2).last()
		} else {
			patchNote.title
		}

		when (this) {
			is TextChannel -> startPublicThreadWithMessage(
				message.id, title
			) { reason = "Thread created for Minecraft update" }

			is NewsChannel -> {
				startPublicThreadWithMessage(
					message.id, title
				) { reason = "Thread created for Minecraft update" }

				message.publish()
			}
		}
	}

	private suspend fun PatchNoteEntry.get() =
		client.get("$BASE_URL/$contentPath").body<PatchNote>()

	class CheckArguments : Arguments() {
		val version by optionalString {
			name = MinecraftTranslations.Command.Mc.Get.Arguments.Version.name
			description = MinecraftTranslations.Command.Mc.Get.Arguments.Version.description
		}
	}

	data class Markdown(
		val body: String,
		val images: List<Image>,
	)

	@Suppress("DataClassShouldBeImmutable")
	data class Image(
		val url: String,
		val alt: String,
	) {
		lateinit var path: Path
	}
}
