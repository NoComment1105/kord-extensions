import java.io.File
import kotlin.system.exitProcess

// language="RegExp"
val BAD_FILES = listOf(
	"\\.github/.*",
	"gradle/wrapper/.*",
	"buildSrc/.*[*/]",
	"[^/]+",

	".*\\.kts",
	".*\\.jar",
	".*\\.bat",
	".*\\.sh",
	".*\\.exe",
).map { it.toRegex(RegexOption.IGNORE_CASE) }

val BASE: String = System.getenv("BASE_SHA")
val TARGET: String = System.getenv("TARGET_SHA")

println("Comparing changed files...")
println(" ⬅️ $BASE")
println(" ➡️ $TARGET")
println()

val process = ProcessBuilder(listOf("git", "--no-pager", "diff", "--name-only", BASE, TARGET))
	.redirectOutput(ProcessBuilder.Redirect.PIPE)
	.redirectError(ProcessBuilder.Redirect.PIPE)
	.start()

val exitCode = process.waitFor()

if (exitCode != 0) {
	exitProcess(-1)
}

val changedFiles = process.inputReader()
	.readText()
	.trim()
	.replace("\r", "")
	.split("\n")

if (changedFiles.isEmpty()) {
	println("No files changed.")

	exitProcess(1)
}

println("🤔 ${changedFiles.size} changed files:")
changedFiles.sorted().forEach { file ->
	if (File(file).isDirectory) {
		println(" 📂 $file")
	} else {
		println(" 📄 $file")
	}
}
println()

val badFiles = changedFiles.filter { file ->
	BAD_FILES.any { regex -> regex.matches(file) }
}

if (badFiles.isNotEmpty()) {
	println("⚠️ ${badFiles.size} bad files:")

	badFiles.sorted().forEach { file ->
		if (File(file).isDirectory) {
			println(" 📂 $file")
		} else {
			println(" 📄 $file")
		}
	}

	exitProcess(1)
} else {
	println("✅ No bad files.")

	exitProcess(0)
}
