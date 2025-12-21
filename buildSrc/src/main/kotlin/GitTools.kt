import org.gradle.api.Project
import org.gradle.process.ExecOperations
import sun.jvmstat.monitor.MonitoredVmUtil.commandLine
import java.io.ByteArrayOutputStream
import javax.inject.Inject

object GitCommitState {
	var branch: String? = null
	var hash: String? = null
}

interface InjectedExecOps {
	@get:Inject
	val execOps: ExecOperations
}

fun Project.runCommand(command: String): String {
	val output = ByteArrayOutputStream()

	providers.exec {
		commandLine(command.split(" "))

		standardOutput = output
	}

	val result = output.toString().trim()

	println(command)
	println(result.prependIndent("-> "))

	return result
}

fun Project.runCommand(command: String, cwd: Any): String {
	val output = providers.exec {
		workingDir(cwd)
		commandLine(command.split(" "))
	}

	val stdout = output.standardOutput.asText.get().trim()
	val stderr = output.standardError.asText.get().trim()

	println("$cwd -> $command")
	println(stdout.prependIndent("OUT -> "))
	println(stderr.prependIndent("ERR -> "))

	return stdout
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
