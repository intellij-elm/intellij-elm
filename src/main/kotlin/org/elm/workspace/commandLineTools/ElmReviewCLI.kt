package org.elm.workspace.commandLineTools

import com.google.gson.stream.JsonReader
import com.intellij.execution.ExecutionException
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.messages.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.tools.ant.taskdefs.Execute.launch
import org.elm.ide.actions.ElmExternalReviewAction
import org.elm.ide.actions.ElmExternalReviewWatchmodeAction
import org.elm.openapiext.*
import org.elm.workspace.ElmProject
import org.elm.workspace.ParseException
import org.elm.workspace.Version
import org.elm.workspace.elmReviewTool
import org.elm.workspace.elmreview.ElmReviewError
import org.elm.workspace.elmreview.elmReviewJsonToMessages
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.io.path.absolutePathString

private val log = logger<ElmReviewCLI>()


/**
 * Interact with external `elm-review` process.
 */
class ElmReviewCLI(val elmReviewExecutablePath: Path) {

    fun runReview(project: Project, elmProject: ElmProject, elmCompiler: ElmCLI?, currentFile: VirtualFile? = null) {
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
                    val msgs = elmReviewJsonToMessages(json).sortedWith(
                        compareBy(
                            { it.path },
                            { it.region.start.line },
                            { it.region.start.column }
                        ))
                    if (currentFile != null) {
                        val predicate: (ElmReviewError) -> Boolean = { it.path == currentFile.pathRelative(project).toString() }
                        val sortedMessages = msgs.filter(predicate) + msgs.filterNot(predicate)
                        sortedMessages
                    } else msgs
                }
                if (!isUnitTestMode) {
                    indicator.text = "review finished"
                    ApplicationManager.getApplication().invokeLater {
                        project.messageBus.syncPublisher(ELM_REVIEW_ERRORS_TOPIC).update(elmProject.projectDirPath, messages, null, 0)
                    }
                }
            } finally {
                Disposer.dispose(processKiller)
            }
        }
    }

    fun watchReview(project: Project, elmProject: ElmProject, elmCompiler: ElmCLI?) {

        val cmd: List<String> = listOf(
            elmReviewExecutablePath.absolutePathString(),
            "--watch",
            "--report=json",
            // This option makes the CLI output non-JSON output, but can be useful to debug what is happening
            // "--debug",
            "--namespace=intellij-elm",
            if (elmCompiler == null) "" else "--compiler=${elmCompiler.elmExecutablePath}"
        )

        executeReviewAsync(project) { indicator ->
            val process = ProcessBuilder(cmd)
                .directory(elmProject.projectDirPath.toFile())
                .start()

            indicator.text = "review started in watchmode"
            val reader = JsonReader(process.inputStream.bufferedReader())
            reader.isLenient = true
            parseReviewJsonStream(reader) { reviewErrors ->
                val msgs = reviewErrors.sortedWith(
                    compareBy(
                        { it.path },
                        { it.regionWatch!!.start!!.line },
                        { it.regionWatch!!.start!!.column }
                    ))
                ApplicationManager.getApplication().invokeLater {
                    val currentFile = DataManager.getInstance().dataContextFromFocusAsync.then { it.getData(PlatformDataKeys.VIRTUAL_FILE) }.blockingGet(4000)
                    val msgsSorted = if (currentFile != null) {
                        val predicate: (ElmReviewWatchError) -> Boolean = { it.path == currentFile.pathRelative(project).toString() }
                        val sortedMessages = msgs.filter(predicate) + msgs.filterNot(predicate)
                        sortedMessages
                    } else msgs
                    if (!isUnitTestMode) {
                        indicator.text = "review updated ${LocalDateTime.now().toString()}"
                        project.messageBus.syncPublisher(ELM_REVIEW_ERRORS_TOPIC).updateWatchmode(elmProject.projectDirPath, msgsSorted, null, 0)
                    }
                }
            }
/*
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy()
                throw RuntimeException("execution timed out: $this")
            }
            if (process.exitValue() != 0) {
                throw RuntimeException("execution failed with code ${process.exitValue()}: $this")
            }
*/
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

val ELM_REVIEW_ERRORS_TOPIC = Topic("elm-review errors", ElmReviewErrorsListener::class.java)

interface ElmReviewErrorsListener {
    fun update(baseDirPath: Path, messages: List<ElmReviewError>, targetPath: String?, offset: Int)
    fun updateWatchmode(baseDirPath: Path, messages: List<ElmReviewWatchError>, targetPath: String?, offset: Int)
}
