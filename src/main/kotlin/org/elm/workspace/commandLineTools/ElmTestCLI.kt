package org.elm.workspace.commandLineTools

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.Result
import org.elm.openapiext.execute
import org.elm.workspace.DEFAULT_TESTS_DIR_NAME
import org.elm.workspace.ParseException
import org.elm.workspace.Version
import java.nio.file.Path

private val log = logger<ElmTestCLI>()

/**
 * Interact with external `elm-test` process.
 */
class ElmTestCLI(private val executablePath: Path) {

    /**
     * Construct a [ProcessHandler] that will run `elm-test` (the caller is responsible for
     * actually invoking the process). The test results will be reported using elm-test's
     * JSON format on stdout.
     *
     * @param elmCompilerPath The path to the Elm compiler.
     * @param elmProjectDirPath The path to the directory containing the Elm project.
     * @param testsDirPath The path to the directory containing the tests.
     */
    fun runTestsProcessHandler(elmCompilerPath: Path, elmProjectDirPath: Path, testsDirPath: Path): ProcessHandler {
        val commandLine = GeneralCommandLine(executablePath.toString(), "--report=json")
                .withWorkDirectory(elmProjectDirPath.toString())
                .withParameters("--compiler", elmCompilerPath.toString())
                .withRedirectErrorStream(true)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

        // By default elm-test will process tests in a folder called "tests", under the current working directory
        // (in this case elmProjectDirPath). If testsDir points at that folder, we don't need to pass anything else in.
        // In other cases we need to supply a path to that folder.
        if (elmProjectDirPath.resolve(DEFAULT_TESTS_DIR_NAME) == testsDirPath) {
            log.debug("""Tests are in default location ("tests") so will run elm-test without argument specifying path.""")
        } else {
            val relativePath = elmProjectDirPath.relativize(testsDirPath).toString()
            log.debug { """Tests are in non-default location: "$relativePath". Will specify this path as argument to elm-test.""" }
            commandLine.withParameters(relativePath)
        }

        return ColoredProcessHandler(commandLine)
    }


    fun queryVersion(): Result<Version> {
        // Output of `elm-test --version` is a single line containing the version number,
        // e.g. `0.19.0-beta9\n`, trimming off the "-betaN" suffix, if present.
        val firstLine = try {
            GeneralCommandLine(executablePath).withParameters("--version")
                    .execute(timeoutInMilliseconds = 3000)
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