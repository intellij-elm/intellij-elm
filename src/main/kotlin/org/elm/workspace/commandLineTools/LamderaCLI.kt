package org.elm.workspace.commandLineTools

import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import org.elm.openapiext.*
import org.elm.workspace.*
import org.elm.workspace.compiler.ElmBuildAction
import org.elm.workspace.compiler.ElmError
import org.elm.workspace.compiler.elmJsonToCompilerMessages
import java.nio.file.Path

private val log = logger<LamderaCLI>()

/**
 * Interact with external `lamdera` process (the compiler, package manager, etc.)
 */
class LamderaCLI(val lamderaExecutablePath: Path) {

    fun make(project: Project, workDir: Path, elmProject: ElmProject?, entryPoints: List<Triple<Path, String?, Int>?>, jsonReport: Boolean = false, currentFile: VirtualFile? = null): Boolean {

        if (entryPoints.isEmpty()) return true

        val entries = entryPoints.filterNotNull()
        val filePathsToCompile = entries.map { it.second.toString() }
        val targetPath = entries.first().second
        val offset = entries.first().third
        val params = (listOf("make") + filePathsToCompile + listOf("--output=/dev/null")).toTypedArray()
        val output = GeneralCommandLine(lamderaExecutablePath)
            .withWorkDirectory(workDir)
            .withParameters(*params)
            .apply { if (jsonReport) addParameter("--report=json") }
            .execute(elmCompilerTool, project)
        val json = output.stderr
        val regex = "\\{.*}".toRegex()
        val cleansedJson = regex.find(json)?.value
        val messages = if (cleansedJson.isNullOrEmpty()) emptyList() else {
            val msgs = elmJsonToCompilerMessages(cleansedJson).sortedWith(
                compareBy(
                    { it.location?.moduleName },
                    { it.location?.region?.start?.line },
                    { it.location?.region?.start?.column }
                ))
            if (currentFile != null) {
                val predicate: (ElmError) -> Boolean = { it.location?.path == currentFile.path }
                val sortedMessages = msgs.filter(predicate) + msgs.filterNot(predicate)
                sortedMessages
            } else msgs
        }
        if (elmProject == null) {
            // from ElmWorkSpaceService
            if (!output.isSuccess) {
                log.error("Failed to install dependencies: Lamdera compiler failed: ${output.stderr}")
                return false
            }
            return true
        } else {
            fun postErrors() = project.messageBus.syncPublisher(ElmBuildAction.ERRORS_TOPIC).update(elmProject.projectDirPath, messages, targetPath!!, offset)
            when {
                isUnitTestMode -> postErrors()
                else -> {
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Elm Compiler")!!
                    toolWindow.show { postErrors() }
                }
            }
        }
        return messages.isEmpty()
    }

    fun queryVersion(project: Project): Result<Version> {
        // Output of `elm --version` is a single line containing the version number (e.g. `0.19.0\n`)
        val firstLine = try {
            GeneralCommandLine(lamderaExecutablePath).withParameters("--version")
                    .execute(elmCompilerTool, project)
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