package org.elm.workspace.commandLineTools

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessNotCreatedException
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.elm.lang.core.psi.ElmFile
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.Result
import org.elm.openapiext.execute
import org.elm.openapiext.isNotSuccess
import org.elm.workspace.ElmApplicationProject
import org.elm.workspace.ElmPackageProject
import org.elm.workspace.ParseException
import org.elm.workspace.Version
import java.nio.file.Path

private val log = logger<ElmFormatCLI>()


/**
 * Interact with external `elm-format` process.
 */
class ElmFormatCLI(private val elmFormatExecutablePath: Path) {

    private fun getFormattedContentOfDocument(elmVersion: Version, document: Document): ProcessOutput {
        val arguments = listOf(
                "--yes",
                "--elm-version=${elmVersion.x}.${elmVersion.y}",
                "--stdin"
        )

        return GeneralCommandLine(elmFormatExecutablePath)
                .withParameters(arguments)
                .execute(document.text)
    }

    sealed class ElmFormatResult(val msg: String, val cause: Throwable? = null) {
        class Success : ElmFormatResult("ok")
        class BadSyntax : ElmFormatResult("elm-format encountered syntax errors that it could not fix")
        class FailedToStart : ElmFormatResult("Failed to launch elm-format. Is the path correct?")
        class UnknownFailure(msg: String? = null, cause: Throwable?) : ElmFormatResult(msg
                ?: "Something went wrong running elm-format", cause)
    }

    fun formatDocumentAndSetText(project: Project, document: Document, version: Version, addToUndoStack: Boolean): ElmFormatResult {
        val processOutput = try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously<ProcessOutput, ExecutionException>({
                getFormattedContentOfDocument(version, document)
            }, "Running elm-format on current file...", true, project)
        } catch (e: ExecutionException) {
            val msg = e.message ?: "unknown"
            return when {
                msg.contains("SYNTAX PROBLEM", ignoreCase = true) -> ElmFormatResult.BadSyntax()
                e is ProcessNotCreatedException -> ElmFormatResult.FailedToStart()
                else -> ElmFormatResult.UnknownFailure(cause = e)
            }
        }

        if (processOutput.isNotSuccess) return ElmFormatResult.UnknownFailure("Process output exit code was non-zero", cause = null)

        val formatted = processOutput.stdout
        val source = document.text

        if (source != formatted) {
            val action = {
                ApplicationManager.getApplication().runWriteAction {
                    document.setText(formatted)
                }
            }

            with(CommandProcessor.getInstance()) {
                when {
                    addToUndoStack -> executeCommand(project, action, "Run elm-format on current file", null, document)
                    else -> runUndoTransparentAction(action)
                }
            }
        }
        return ElmFormatResult.Success()
    }


    fun queryVersion(): Result<Version> {
        // Output of `elm-format` is multiple lines where the first line is something like 'elm-format 0.8.1'.
        // NOTE: `elm-format` does not currently support a `--version` argument, so this is going to be brittle.
        val firstLine = try {
            GeneralCommandLine(elmFormatExecutablePath)
                    .execute(timeoutInMilliseconds = 3000)
                    .stdoutLines
                    .firstOrNull()
        } catch (e: ExecutionException) {
            return Result.Err("failed to run elm-format: ${e.message}")
        } ?: return Result.Err("no output from elm-format")

        return try {
            Result.Ok(Version.parse(firstLine))
        } catch (e: ParseException) {
            Result.Err("invalid elm-format version: ${e.message}")
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