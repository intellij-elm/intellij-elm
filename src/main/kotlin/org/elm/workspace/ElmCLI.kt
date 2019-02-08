package org.elm.workspace

import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.Disposable
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.execute
import org.elm.openapiext.withWorkDirectory
import java.nio.file.Path

class ElmCLI(private val elmExecutablePath: Path) {

    // TODO [kl] allow the caller to specify the main entry point Elm file

    fun make(owner: Disposable, elmProject: ElmProject): ProcessOutput {
        val workDir = elmProject.manifestPath.parent
        return GeneralCommandLine(elmExecutablePath)
                .withWorkDirectory(workDir)
                .withParameters("make", "src/Main.elm", "--output=/dev/null")
                .execute(owner, ignoreExitCode = true)
    }

    fun installDeps(owner: Disposable, elmProjectManifestPath: Path): ProcessOutput {
        // Elm 0.19 does not have a way to install dependencies directly,
        // so we have to compile an empty file to make it work.
        val workDir = elmProjectManifestPath.parent
        return GeneralCommandLine(elmExecutablePath)
                .withWorkDirectory(workDir)
                .withParameters("make", "./Main.elm", "--output=/dev/null")
                .execute(owner, ignoreExitCode = false)
    }

}