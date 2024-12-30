/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

@file:OptIn(NotTranslated::class)

package dev.kordex.test.bot.extensions

import dev.kordex.core.annotations.NotTranslated
import dev.kordex.core.commands.application.slash.publicSubCommand
import dev.kordex.core.events.KordExEvent
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.extensions.publicSlashCommand
import dev.kordex.core.i18n.toKey
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

public class BenchmarkExtension : Extension() {
	override val name: String = "benchmark"

	private val logger = KotlinLogging.logger { }

	private val mutex = Mutex()
	private var currentSent: Int = 0
	private var currentReceived: Int = 0
	private var totalSent: Int = 0
	private var totalReceived: Int = 0

	public var benchmarking: Boolean = false

	override suspend fun setup() {
		publicSlashCommand {
			name = "benchmark".toKey()
			description = "Benchmarking commands".toKey()

			publicSubCommand {
				name = "events-low-level".toKey()
				description = "Benchmark low-level event throughput".toKey()

				check {
					failIf("Already benchmarking, please wait.") { benchmarking }
				}

				action {
					benchmarking = true

					val maximumEvents: Int = 10_000_000

					edit {
						content = "Benchmarking $maximumEvents events. This will take a while!"
					}

					var currentSent = 0
					var currentReceived = 0
					var totalSent = 0
					var totalReceived = 0

					var resultIndex: Int = 1
					var totalTime = Duration.ZERO

					bot.kordRef.launch {
						lateinit var job: Job

						logger.info { "== Starting event throughput benchmark ==" }

						totalTime = measureTime {
							val mutex = Mutex()

							job = bot.on<BenchmarkingEvent> {
								bot.kordRef.launch {
									mutex.withLock {
										currentReceived++
										totalReceived++
									}
								}
							}

							delay(1.seconds)

							for (index in 0 until maximumEvents) {
								bot.send(BenchmarkingEvent(index))

								currentSent++
								totalSent++
							}

							while (totalReceived < maximumEvents) {
								delay(10.milliseconds)
							}

							job.cancel()

							benchmarking = false
						}
					}

					var resultList: MutableList<String> = mutableListOf()

					do {
						logger.info {
							"Step $resultIndex - Current: roughly $currentSent sent/$currentReceived received; " +
								"Total: $totalSent sent/$totalReceived received"
						}

						resultList.add(
							"**Step $resultIndex** - Current: _roughly_ `$currentSent` sent, `$currentReceived` " +
								"received; Total: `$totalSent` sent, `$totalReceived` received"
						)

						edit {
							content = "Benchmarking $maximumEvents events. This will take a while!\n\n" +
								resultList.takeLast(15).joinToString("\n")
						}

						currentSent = 0
						currentReceived = 0
						resultIndex++

						delay(5.seconds)
					} while (benchmarking)

					val finishedTime = totalTime - 1.seconds

					logger.info {
						"== Event throughput benchmark complete, $maximumEvents handled in " +
							"${finishedTime.inWholeMilliseconds} milliseconds =="
					}

					respond {
						content = "Benchmarking finished - $totalSent sent, $totalReceived received.\n" +
							"Total duration for $maximumEvents events: " +
							"${finishedTime.inWholeMilliseconds} milliseconds."
					}
				}
			}

			publicSubCommand {
				name = "events-high-level".toKey()
				description = "Benchmark high-level event throughput".toKey()

				check {
					failIf("Already benchmarking, please wait.") { benchmarking }
				}

				action {
					benchmarking = true

					val maximumEvents: Int = 10_000_000

					edit {
						content = "Benchmarking $maximumEvents events. This will take a while!"
					}

					currentSent = 0
					currentReceived = 0
					totalSent = 0
					totalReceived = 0

					var resultIndex: Int = 1
					var totalTime = Duration.ZERO

					bot.kordRef.launch {
						logger.info { "== Starting event throughput benchmark ==" }

						totalTime = measureTime {
							for (index in 0 until maximumEvents) {
								bot.send(BenchmarkingEvent(index))

								currentSent++
								totalSent++
							}

							while (totalReceived < maximumEvents) {
								delay(10.milliseconds)
							}

							benchmarking = false
						}
					}

					var resultList: MutableList<String> = mutableListOf()

					do {
						logger.info {
							"Step $resultIndex - Current: roughly $currentSent sent/$currentReceived received; " +
								"Total: $totalSent sent/$totalReceived received"
						}

						resultList.add(
							"**Step $resultIndex** - Current: _roughly_ `$currentSent` sent, `$currentReceived` " +
								"received; Total: `$totalSent` sent, `$totalReceived` received"
						)

						edit {
							content = "Benchmarking $maximumEvents events. This will take a while!\n\n" +
								resultList.takeLast(15).joinToString("\n")
						}

						currentSent = 0
						currentReceived = 0
						resultIndex++

						delay(5.seconds)
					} while (benchmarking)

					logger.info {
						"== Event throughput benchmark complete, $maximumEvents handled in " +
							"${totalTime.inWholeMilliseconds} milliseconds =="
					}

					respond {
						content = "Benchmarking finished - $totalSent sent, $totalReceived received.\n" +
							"Total duration for $maximumEvents events: " +
							"${totalTime.inWholeMilliseconds} milliseconds."
					}
				}
			}
		}

		event<BenchmarkingEvent> {
			check {
				failIfNot(benchmarking)
			}

			action {
				mutex.withLock {
					currentReceived++
					totalReceived++
				}
			}
		}
	}

	public class BenchmarkingEvent(index: Int) : KordExEvent
}
