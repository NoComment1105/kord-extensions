import org.gradle.api.Project
import java.io.ByteArrayOutputStream

object GitCommitState {
	var branch: String? = null
	var hash: String? = null
}

fun Project.runCommand(command: String): String {
	val output = ByteArrayOutputStream()

	exec {
		commandLine(command.split(" "))

		standardOutput = output
	}

	val result = output.toString().trim()

	println(command)
	println(result.prependIndent("-> "))

	return result
}

fun Project.runCommand(command: String, cwd: Any): String {
	val output = ByteArrayOutputStream()

	exec {
		workingDir(cwd)
		commandLine(command.split(" "))

		standardOutput = output
		errorOutput = output
	}

	val result = output.toString().trim()

	println("$cwd -> $command")
	println(result.prependIndent("-> "))

	return output.toString().trim()
}

fun Project.getCurrentGitBranch(): String {  // https://gist.github.com/lordcodes/15b2a4aecbeff7c3238a70bfd20f0931
	if (GitCommitState.branch == null) {
		GitCommitState.branch = "Unknown branch"

		try {
			GitCommitState.branch = runCommand("git rev-parse --abbrev-ref HEAD").trim()
		} catch (t: Throwable) {
			println(t)
		}
	}

	return GitCommitState.branch!!
}

fun Project.getCurrentGitHash(): String {  // https://gist.github.com/lordcodes/15b2a4aecbeff7c3238a70bfd20f0931
	if (GitCommitState.hash == null) {
		GitCommitState.hash = "unknown"

		try {
			GitCommitState.hash = runCommand("git rev-parse --short HEAD").trim()
		} catch (t: Throwable) {
			println(t)
		}
	}

	return GitCommitState.hash!!
}
