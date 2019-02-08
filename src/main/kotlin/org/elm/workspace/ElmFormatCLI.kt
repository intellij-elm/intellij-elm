package org.elm.workspace

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.execute
import org.elm.openapiext.pathAsPath
import org.elm.openapiext.withWorkDirectory
import java.nio.file.Path

class ElmFormatCLI(private val elmFormatExecutablePath: Path) {

    fun reformatFile(project: Project, elmVersion: Version, file: VirtualFile, owner: Disposable = project): ProcessOutput {
        val arguments = listOf(
                "--yes",
                "--elm-version",
                "${elmVersion.x}.${elmVersion.y}",
                file.path
        )
        return GeneralCommandLine(elmFormatExecutablePath)
                .withWorkDirectory(file.parent.pathAsPath)
                .withParameters(arguments)
                .execute(owner)
    }
}