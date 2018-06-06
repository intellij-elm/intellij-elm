package org.elm.workspace

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.exists
import com.intellij.util.text.SemVer
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.checkIsBackgroundThread
import org.elm.openapiext.modules
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val log = Logger.getInstance(ElmToolchain::class.java)


data class ElmToolchain(val binDirPath: Path) {


    val presentableLocation: String =
            pathToExecutable(ELM_BINARY).toString()


    val elmHomePath: String
        get() {
            /*
            The Elm compiler first checks the ELM_HOME environment variable. If not found,
            it will fallback to the path returned by Haskell's `System.Directory.getAppUserDataDirectory`
            function. That function behaves as follows:

            - On Unix-like systems, the path is ~/.<app>.
            - On Windows, the path is %APPDATA%/<app> (e.g. C:/Users/<user>/AppData/Roaming/<app>)

            IntelliJ's FileUtil.expandUserHome() uses the JVM's `user.home` system property to
            determine the home directory.

            - On Unix-like systems, the path is /Users/<user>
            - on Windows, the path is C:/Users/<user>

            Note that the Haskell and Java functions do slightly different things.
            */
            val elmHomeVar = System.getenv("ELM_HOME")
            if (elmHomeVar != null && Paths.get(elmHomeVar).exists())
                return elmHomeVar

            return when {
                SystemInfo.isUnix -> FileUtil.expandUserHome("~/.elm")
                SystemInfo.isMac -> FileUtil.expandUserHome("~/.elm")
                SystemInfo.isWindows -> FileUtil.expandUserHome("~/AppData/Roaming/elm")
                else -> error("Unsupported platform")
            }
        }


    // TODO [kl] scrape from the Elm compiler
    val compilerVersion = "0.19.0"


    fun packageRootDir(name: String, version: String): VirtualFile? {
        val path = "$elmHomePath/$compilerVersion/package/$name/$version/"
        return LocalFileSystem.getInstance().findFileByPath(path)
    }

    fun looksLikeValidToolchain(): Boolean {
        return Files.isExecutable(pathToExecutable(ELM_BINARY))
    }

    fun pathToExecutable(toolName: String): Path {
        val executableName = if (SystemInfo.isWindows) "$toolName.exe" else toolName
        return binDirPath.resolve(executableName).toAbsolutePath()
    }

    fun queryCompilerVersion(): SemVer? {
        checkIsBackgroundThread()
        // Output of `elm --version` is a single line containing the version number (e.g. `0.19.0\n`)
        return GeneralCommandLine(pathToExecutable(ELM_BINARY))
                .withParameters("--version")
                .runExecutable()
                ?.firstOrNull()
                ?.let { SemVer.parseFromText(it) }
    }

    companion object {
        val ELM_BINARY = "elm"
        val ELM_JSON = "elm.json"

        val MIN_SUPPORTED_COMPILER_VERSION = SemVer("0.19.0", 0, 19, 0)

        fun suggest(project: Project): ElmToolchain? {
            return binDirSuggestions(project).mapNotNull {
                val candidate = ElmToolchain(it.toPath().toAbsolutePath())
                candidate.takeIf { it.looksLikeValidToolchain() }
            }.firstOrNull()
        }
    }
}

private fun binDirSuggestions(project: Project) =
        sequenceOf(
                suggestionsFromNPM(project),
                suggestionsFromPath(),
                suggestionsForMac(),
                suggestionsForWindows(),
                suggestionsForUnix()
        ).flatten()


private fun suggestionsFromNPM(project: Project): Sequence<File> {
    return project.modules
            .asSequence()
            .flatMap { ModuleRootManager.getInstance(it).contentRoots.asSequence() }
            .flatMap {
                FileUtil.fileTraverser(File(it.path))
                        .filter { it.name == "node_modules" && it.isDirectory }
                        .bfsTraversal()
                        .asSequence()
            }.map { Paths.get(it.absolutePath).resolve(".bin").toFile() }
}

private fun suggestionsFromPath(): Sequence<File> {
    return System.getenv("PATH").orEmpty()
            .split(File.pathSeparator)
            .asSequence()
            .filter { !it.isEmpty() }
            .map(::File)
            .filter { it.isDirectory }

}

private fun suggestionsForMac(): Sequence<File> {
    if (!SystemInfo.isMac) return emptySequence()
    return sequenceOf(File("/usr/local/bin"))
}

private fun suggestionsForUnix(): Sequence<File> {
    if (!SystemInfo.isUnix) return emptySequence()
    return sequenceOf(File("/usr/local/bin"))
}

private fun suggestionsForWindows(): Sequence<File> {
    if (!SystemInfo.isWindows) return emptySequence()

    // TODO [kl] find out where the Elm installer puts its binaries on Windows
    return emptySequence()
}


private fun GeneralCommandLine.runExecutable(): List<String>? {
    val procOut = try {
        val timeoutMs = 1000
        CapturingProcessHandler(this).runProcess(timeoutMs)
    } catch (e: ExecutionException) {
        log.warn("Failed to run executable!", e)
        return null
    }

    if (procOut.exitCode != 0 || procOut.isCancelled || procOut.isTimeout)
        return null

    return procOut.stdoutLines
}


fun SemVer.isOlderThan(other: SemVer) =
        compareTo(other) == -1