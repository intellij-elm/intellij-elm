package org.elm.workspace.commandLineTools

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.Result
import org.elm.openapiext.execute
import org.elm.openapiext.withWorkDirectory
import org.elm.workspace.ParseException
import org.elm.workspace.Version
import java.nio.file.Path

/**
 * Interact with external `elm` process (the compiler, package manager, etc.)
 */
class ElmCLI(private val elmExecutablePath: Path) {

    fun make(owner: Disposable, workDir: Path, path: Path, jsonReport: Boolean = false): ProcessOutput {
        return GeneralCommandLine(elmExecutablePath)
                .withWorkDirectory(workDir)
                .withParameters("make", path.toString(), "--output=/dev/null")
                .apply { if (jsonReport) addParameter("--report=json") }
                .execute(owner, ignoreExitCode = true)
    }

    fun queryVersion(): Result<Version> {
        // Output of `elm --version` is a single line containing the version number (e.g. `0.19.0\n`)
        val firstLine = try {
            GeneralCommandLine(elmExecutablePath).withParameters("--version")
                    .execute(timeoutInMilliseconds = 3000)
                    .stdoutLines
                    .firstOrNull()
        } catch (e: ExecutionException) {
            return Result.Err("failed to run elm: ${e.message}")
        }

        if (firstLine == null) {
            return Result.Err("no output from elm")
        }

        return try {
            Result.Ok(Version.parse(firstLine))
        } catch (e: ParseException) {
            Result.Err("could not parse Elm version: ${e.message}")
        }
    }
}