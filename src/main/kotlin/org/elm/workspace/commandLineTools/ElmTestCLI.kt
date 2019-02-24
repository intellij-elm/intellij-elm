package org.elm.workspace.commandLineTools

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.Result
import org.elm.openapiext.execute
import org.elm.workspace.ParseException
import org.elm.workspace.Version
import java.nio.file.Path

/**
 * Interact with external `elm-test` process.
 */
class ElmTestCLI(private val executablePath: Path) {

    /**
     * Construct a [ProcessHandler] that will run `elm-test` (the caller is responsible for
     * actually invoking the process). The test results will be reported using elm-test's
     * JSON format on stdout.
     */
    fun runTestsProcessHandler(elmCompilerPath: Path, elmProjectDirPath: String): ProcessHandler {
        val commandLine = GeneralCommandLine(executablePath.toString(), "--report=json")
                .withWorkDirectory(elmProjectDirPath)
                .withParameters("--compiler", elmCompilerPath.toString())
                .withRedirectErrorStream(true)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        return ColoredProcessHandler(commandLine)
    }


    fun queryVersion(): Result<Version> {
        // Output of `elm-test --version` is a single line containing the version number,
        // e.g. `0.19.0-beta9\n`, trimming off the "-betaN" suffix, if present.
        val firstLine = try {
            GeneralCommandLine(executablePath).withParameters("--version")
                    .execute(timeoutInMilliseconds = 1500)
                    .stdoutLines
                    .firstOrNull()
        } catch (e: ExecutionException) {
            return Result.Err("failed to run elm-test: ${e.message}")
        }

        if (firstLine == null) {
            return Result.Err("no output from elm-test")
        }

        val trimmedFirstLine = firstLine.takeWhile { it != '-' }

        return try {
            Result.Ok(Version.parse(trimmedFirstLine))
        } catch (e: ParseException) {
            Result.Err("could not parse elm-test version: ${e.message}")
        }
    }
}