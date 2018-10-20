package org.elm.workspace

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.Disposer
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.withWorkDirectory
import java.nio.file.Path

class ElmCLI(private val elmExecutablePath: Path) {

    // TODO [kl] allow the caller to specify the main entry point Elm file

    fun make(owner: Disposable, elmProject: ElmProject): ProcessOutput {
        val workDir = elmProject.manifestPath.parent
        val commandLine = GeneralCommandLine(elmExecutablePath)
                .withWorkDirectory(workDir)
                .withParameters("make", "src/Main.elm", "--output=/dev/null")
        return execute(commandLine, owner, ignoreExitCode = true)
    }

    fun installDeps(owner: Disposable, elmProjectManifestPath: Path): ProcessOutput {
        // Elm 0.19 does not have a way to install dependencies directly,
        // so we have to compile an empty file to make it work.
        val workDir = elmProjectManifestPath.parent
        val commandLine = GeneralCommandLine(elmExecutablePath)
                .withWorkDirectory(workDir)
                .withParameters("make", "./Main.elm", "--output=/dev/null")
        return execute(commandLine, owner, ignoreExitCode = false)
    }

    private fun execute(cmdLine: GeneralCommandLine, owner: Disposable, ignoreExitCode: Boolean = false): ProcessOutput {
        val handler = CapturingProcessHandler(cmdLine)
        val processKiller = Disposable {
            handler.destroyProcess()
        }

        val alreadyDisposed = runReadAction {
            if (Disposer.isDisposed(owner)) {
                true
            } else {
                Disposer.register(owner, processKiller)
                false
            }
        }

        if (alreadyDisposed) {
            if (ignoreExitCode) {
                return ProcessOutput().apply { setCancelled() }
            } else {
                throw ExecutionException("Cargo command failed to start")
            }
        }

        val output = try {
            handler.runProcess()
        } finally {
            Disposer.dispose(processKiller)
        }

        if (!ignoreExitCode && output.exitCode != 0) {
            throw ExecutionException("""
                    Failed to execute external program (exit code ${output.exitCode}).
                    ${cmdLine.commandLineString}
                    stdout : ${output.stdout}
                    stderr : ${output.stderr}
                    """.trimIndent()
            )
        }
        return output
    }
}