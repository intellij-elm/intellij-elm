package org.elm.workspace.commandLineTools

import com.google.gson.stream.JsonReader
import com.intellij.execution.ExecutionException
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.messages.Topic
import org.elm.openapiext.*
import org.elm.workspace.*
import org.elm.workspace.elmreview.ElmReviewError
import org.elm.workspace.elmreview.parseReviewJsonStream
import org.elm.workspace.elmreview.readErrorReport
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private val log = logger<ElmReviewCLI>()


/**
 * Interact with external `elm-review` process.
 */
class ElmReviewCLI(val elmReviewExecutablePath: Path) {

    fun runReview(project: Project, elmProject: ElmProject, elmCompiler: ElmCLI?, currentFile: VirtualFile? = null) {

        // This option makes the CLI output non-JSON output, but can be useful to debug what is happening
        // "--debug",

        val arguments = listOf("--report=json", "--namespace=intellij-elm") +
                if (elmProject is ElmApplicationProject) "--config=." else "" +
                        if (elmCompiler == null) "" else "--compiler=${elmCompiler.elmExecutablePath}"

        val generalCommandLine = GeneralCommandLine(elmReviewExecutablePath).withWorkDirectory(elmProject.projectDirPath.toString()).withParameters(arguments)

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
                if (output.exitCode != 0) {
                    log.warn("elm-review exited with code ${output.exitCode} and output ${output.stdoutLines}")
                }
                val json = output.stderr.ifEmpty {
                    output.stdout
                }
                val messages = if (json.isEmpty())
                    emptyList()
                else {
                    val reader = JsonReader(json.byteInputStream().bufferedReader())
                    reader.isLenient = true
                    val msgs = reader.readErrorReport().sortedWith(
                        compareBy(
                            { it.path },
                            { it.region!!.start!!.line },
                            { it.region!!.start!!.column }
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

        // This option makes the CLI output non-JSON output, but can be useful to debug what is happening
        // "--debug",

        val arguments = listOf("--watch", "--report=json", "--namespace=intellij-elm") +
                if (elmProject is ElmApplicationProject) "--config=." else "" +
                        if (elmCompiler == null) "" else "--compiler=${elmCompiler.elmExecutablePath}"

        val command: List<String> = listOf(elmReviewExecutablePath.absolutePathString(), *arguments.toTypedArray())

        executeReviewAsync(project) { indicator ->

            val elmReviewService = project.getService(ElmReviewService::class.java)
            elmReviewService.activeWatchmodeProcess?.destroyForcibly()
            val process = startProcess(command, elmProject, project)
            elmReviewService.activeWatchmodeProcess = process

            Disposer.register(project) { process.destroyForcibly() }

            try {
                indicator.text = "review started in watchmode"
                val reader = JsonReader(process.inputStream.bufferedReader())
                reader.isLenient = true
                val exitCode = parseReviewJsonStream(reader, process) { reviewErrors ->
                    val msgs = reviewErrors.filterNot { it.suppressed != null && it.suppressed!! }.sortedWith(errorComparator(reviewErrors))
                    if (msgs.isNotEmpty()) {
                        ApplicationManager.getApplication().invokeLater {
                            val currentDoc = FileEditorManager.getInstance(project).selectedTextEditor?.document
                            val msgsSorted =
                                if (currentDoc != null) {
                                    val path = PsiDocumentManager.getInstance(project).getPsiFile(currentDoc)?.originalFile?.virtualFile?.pathRelative(project)
                                    if (path != null) {
                                        val pathFilter: (ElmReviewError) -> Boolean = { it.path == path.toString() }
                                        msgs.filter(pathFilter) + msgs.filterNot(pathFilter)
                                    } else msgs
                                } else msgs
                            if (!isUnitTestMode) {
                                indicator.text = "review has ${msgs.size} messages"
                                project.messageBus.syncPublisher(ELM_REVIEW_ERRORS_TOPIC).update(elmProject.projectDirPath, msgsSorted, null, 0)
                            }
                        }
                    }
                }
                if (exitCode != 0) log.warn("elm-review exited with code $exitCode")
            } finally {
                process.destroyForcibly()
            }
        }
    }

    private fun errorComparator(reviewErrors: List<ElmReviewError>): Comparator<ElmReviewError> {
        return if (reviewErrors.isEmpty() || reviewErrors[0].region == null)
            compareBy { it.path }
        else
            compareBy({ it.path }, { it.region!!.start!!.line }, { it.region!!.start!!.column })
    }

    private fun startProcess(cmd: List<String>, elmProject: ElmProject, project: Project): Process =
        ProcessBuilder(cmd)
            .directory(elmProject.projectDirPath.toFile())
            .start()

    fun queryVersion(project: Project): Result<Version> {
        val firstLine = try {
            val arguments: List<String> = listOf("--version")
            GeneralCommandLine(elmReviewExecutablePath)
                .withParameters(arguments)
                .execute(elmReviewTool, project)
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
}
