package org.elm.workspace.commandLineTools

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.elm.lang.core.psi.ElmFile
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.Result
import org.elm.openapiext.execute
import org.elm.workspace.*
import java.nio.file.Path

private val log = logger<ElmReviewCLI>()


/**
 * Interact with external `elm-review` process.
 */
class ElmReviewCLI(private val elmReviewExecutablePath: Path) {

    fun runReview(project: Project, elmProject: ElmProject, elmCompiler: ElmCLI?): ProcessOutput {
        val arguments: List<String> = listOf(
                "--report=json",
                // This option makes the CLI output non-JSON output, but can be useful to debug what is happening
                // "--debug",
                "--namespace=intellij-elm",
                if (elmCompiler == null) "" else "--compiler=${elmCompiler.elmExecutablePath}"
        )
        return GeneralCommandLine(elmReviewExecutablePath)
                .withWorkDirectory(elmProject.projectDirPath.toString())
                .withParameters(arguments)
                .execute(elmReviewTool, project, timeoutInMilliseconds = 20000, ignoreExitCode = true)

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

    companion object {
        fun getElmVersion(project: Project, file: VirtualFile): Version? {
            val psiFile = ElmFile.fromVirtualFile(file, project) ?: return null

            return when (val elmProject = psiFile.elmProject) {
                is ElmApplicationProject -> elmProject.elmVersion
                is ElmPackageProject -> elmProject.elmVersion.low
                else -> return null
            }
        }
    }
}