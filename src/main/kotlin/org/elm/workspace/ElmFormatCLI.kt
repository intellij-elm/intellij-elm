package org.elm.workspace

import com.intellij.execution.ExecutionException
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.elm.lang.core.psi.ElmFile
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.execute
import org.elm.openapiext.isSuccess
import org.elm.openapiext.toPsiFile
import java.nio.file.Path

class ElmFormatCLI(val elmFormatExecutablePath: Path) {

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

    fun formatDocumentAndSetText(project: Project, document: Document, version: Version, addToUndoStack: Boolean) {

        val result = ProgressManager.getInstance().runProcessWithProgressSynchronously<ProcessOutput, ExecutionException>({
            getFormattedContentOfDocument(version, document)
        }, "Running elm-format on current file...", true, project)

        if (result.isSuccess) {
            val formatted = result.stdout
            val source = document.text

            if (source != formatted) {

                val writeAction = {
                    ApplicationManager.getApplication().runWriteAction {
                        document.setText(formatted)
                    }
                }

                if (addToUndoStack) {
                    CommandProcessor.getInstance().executeCommand(project, writeAction, "Run elm-format on current file", null, document)
                } else {
                    CommandProcessor.getInstance().runUndoTransparentAction(writeAction)
                }
            }
        }
    }


    companion object {

        fun getElmVersion(project: Project, file: VirtualFile): Version? {
            val psiFile = (file.toPsiFile(project) as? ElmFile) ?: return null

            return when (val elmProject = psiFile.elmProject) {
                is ElmApplicationProject -> elmProject.elmVersion
                is ElmPackageProject -> elmProject.elmVersion.low
                else -> return null
            }
        }
    }
}