package org.elm.workspace.commandLineTools

import com.intellij.execution.ExecutionException
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import org.elm.openapiext.*
import org.elm.workspace.ElmProject
import org.elm.workspace.ParseException
import org.elm.workspace.Version
import org.elm.workspace.compiler.ElmBuildAction
import org.elm.workspace.compiler.elmJsonToCompilerMessages
import java.nio.file.Path

/**
 * Interact with external `elm` process (the compiler, package manager, etc.)
 */
class ElmCLI(val elmExecutablePath: Path) {

    fun make(project: Project, workDir: Path, elmProject: ElmProject?, entryPoints: List<Triple<Path, String?, Int>?>, jsonReport: Boolean = false): Boolean {

        // Lamdera make 2 entryPoints in 1 call !
        val entries = entryPoints.filterNotNull()
        val filePathsToCompile = entries.map { it.first.toString() }
        val targetPath = entries.first().second
        val offset = entries.first().third
        val params = (listOf("make") + filePathsToCompile + listOf("--output=/dev/null")).toTypedArray()
        val output = GeneralCommandLine(elmExecutablePath)
            .withWorkDirectory(workDir)
            .withParameters(*params)
            .apply { if (jsonReport) addParameter("--report=json") }
            .execute(project, ignoreExitCode = true)
        val json = output.stderr
        val regex = "\\{.*}".toRegex()
        val cleansedJson = regex.find(json)?.value
        val messages = if (cleansedJson.isNullOrEmpty()) emptyList() else {
            elmJsonToCompilerMessages(cleansedJson).sortedWith(
                compareBy(
                    { it.location?.moduleName },
                    { it.location?.region?.start?.line },
                    { it.location?.region?.start?.column }
                ))
        }
        if (elmProject == null) {
            // from ElmWorkSpaceService
            if (!output.isSuccess) {
                // TODO Lamdera
                //  org.elm.workspace.log.error("Failed to install deps: Elm compiler failed: ${output.stderr}")
                return false
            }
            return true
        } else {
            fun postErrors() = project.messageBus.syncPublisher(ElmBuildAction.ERRORS_TOPIC).update(elmProject.projectDirPath, messages, targetPath!!, offset)

            when {
                isUnitTestMode -> postErrors()
                else -> ToolWindowManager.getInstance(project).getToolWindow("Elm Compiler")?.show {
                    postErrors()
                }
            }
        }
        return true
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