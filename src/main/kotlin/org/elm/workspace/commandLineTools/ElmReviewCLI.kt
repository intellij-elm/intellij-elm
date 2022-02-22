package org.elm.workspace.commandLineTools

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import org.elm.ide.actions.ElmExternalReviewAction
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.Result
import org.elm.openapiext.execute
import org.elm.openapiext.isUnitTestMode
import org.elm.workspace.ElmProject
import org.elm.workspace.ParseException
import org.elm.workspace.Version
import org.elm.workspace.elmReviewTool
import org.elm.workspace.elmreview.elmReviewJsonToMessages
import java.nio.file.Path

private val log = logger<ElmReviewCLI>()


/**
 * Interact with external `elm-review` process.
 */
class ElmReviewCLI(val elmReviewExecutablePath: Path) {

    fun runReview(project: Project, elmProject: ElmProject, elmCompiler: ElmCLI?) {
        val arguments: List<String> = listOf(
            "--report=json",
            // This option makes the CLI output non-JSON output, but can be useful to debug what is happening
            // "--debug",
            "--namespace=intellij-elm",
            if (elmCompiler == null) "" else "--compiler=${elmCompiler.elmExecutablePath}"
        )
        val generalCommandLine = GeneralCommandLine(elmReviewExecutablePath)
            .withWorkDirectory(elmProject.projectDirPath.toString())
            .withParameters(arguments)

        executeReviewAsync(project) { indicator ->

            indicator.text = "reviewing ${elmProject.projectDirPath}"
            val handler = CapturingProcessHandler(generalCommandLine)
            val processKiller = Disposable { handler.destroyProcess() }

            Disposer.register(project, processKiller)
            try {
                val output = handler.runProcess()
                val alreadyDisposed = runReadAction { project.isDisposed }
                if (alreadyDisposed) {
                    throw ExecutionException("External command failed to start")
                }
/*
                    if (output.exitCode != 0) {
                        throw ExecutionException(errorMessage(generalCommandLine, output))
                    }
*/
                val json = output.stderr.ifEmpty {
                    output.stdout
                }
                val messages = if (json.isEmpty()) emptyList() else {
                    elmReviewJsonToMessages(json).sortedWith(
                        compareBy(
                            { it.path },
                            { it.region.start.line },
                            { it.region.start.column }
                        ))
                }
                if (!isUnitTestMode) {
                    indicator.text = "review finished"
                    ApplicationManager.getApplication().invokeLater {
                        project.messageBus.syncPublisher(ElmExternalReviewAction.ERRORS_TOPIC).update(elmProject.projectDirPath, messages, null, 0)
                    }
                }
            } finally {
                Disposer.dispose(processKiller)
            }
        }
    }

    fun queryVersion(): Result<Version> {
        val firstLine = try {
            val arguments: List<String> = listOf("--version")
            GeneralCommandLine(elmReviewExecutablePath)
                .withParameters(arguments)
                .execute(timeoutInMilliseconds = 3000)
                .stdoutLines
                .firstOrNull()
        } catch (e: ExecutionException) {
            return Result.Err("failed to run elm-review: ${e.message}")
        } ?: return Result.Err("no output from elm-review")

        return try {
            Result.Ok(Version.parse(firstLine))
        } catch (e: ParseException) {
            Result.Err("invalid elm-review version: ${e.message}")
        }
    }
}

@Throws(ExecutionException::class)
fun executeReviewAsync(
    project: Project,
    task: (indicator: ProgressIndicator) -> Unit
) {
    if (!isUnitTestMode) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(elmReviewTool)!!
        toolWindow.show()
    }
    runBackgroundableTask(elmReviewTool, project, true, task)
}

