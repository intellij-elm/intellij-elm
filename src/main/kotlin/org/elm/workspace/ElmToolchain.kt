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
import com.intellij.util.io.isDirectory
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
    constructor(binDirPath: String) : this(Paths.get(binDirPath))

    val presentableLocation: String
        get() = (elmCompilerPath ?: binDirPath.resolve("elm")).toString()

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

    val elmCompilerPath: Path? get() {
        return elmCompilerNameSuggestions()
                .map { binDirPath.resolve(it) }
                .firstOrNull { Files.isExecutable(it) }
    }

    fun looksLikeValidToolchain(): Boolean = elmCompilerPath != null

    fun packageRootDir(name: String, version: String): VirtualFile? {
        // TODO [kl] scrape the version from the Elm compiler
        // it's ok to assume 19 here because this will never be called from 0.18 code,
        // but even this assumption will not be safe once future 19 releases are made.
        val compilerVersion = "0.19.0"

        val path = "$elmHomePath/$compilerVersion/package/$name/$version/"
        return LocalFileSystem.getInstance().findFileByPath(path)
    }

    fun queryCompilerVersion(): SemVer? {
        checkIsBackgroundThread()
        val elm = elmCompilerPath ?: return null
        // Output of `elm --version` is a single line containing the version number (e.g. `0.19.0\n`)
        return GeneralCommandLine(elm)
                .withParameters("--version")
                .runExecutable()
                ?.firstOrNull()
                ?.let { SemVer.parseFromText(it) }
    }

    companion object {
        const val ELM_JSON = "elm.json"
        const val ELM_LEGACY_JSON = "elm-package.json" // TODO [drop 0.18]

        val MIN_SUPPORTED_COMPILER_VERSION = SemVer("0.18.0", 0, 18, 0)

        /** Suggest a toolchain that exists in in any standard location */
        fun suggest(project: Project): ElmToolchain? {
            return binDirSuggestions(project)
                    .map { ElmToolchain(it.toAbsolutePath()) }
                    .firstOrNull { it.looksLikeValidToolchain() }
        }
    }
}

// Look for both installed and npm versions of the binary
private fun elmCompilerNameSuggestions() =
        if (SystemInfo.isWindows) sequenceOf("elm.exe", "elm.cmd", "elm")
        else sequenceOf("elm")

private fun binDirSuggestions(project: Project) =
        sequenceOf(
                suggestionsFromNPM(project),
                suggestionsFromPath(),
                suggestionsForMac(),
                suggestionsForWindows(),
                suggestionsForUnix()
        ).flatten()


private fun suggestionsFromNPM(project: Project): Sequence<Path> {
    return project.modules
            .asSequence()
            .flatMap { ModuleRootManager.getInstance(it).contentRoots.asSequence() }
            .flatMap {
                FileUtil.fileTraverser(File(it.path))
                        .filter { it.name == "node_modules" && it.isDirectory }
                        .bfsTraversal()
                        .asSequence()
            }.map { Paths.get(it.absolutePath, ".bin") }
}

private fun suggestionsFromPath(): Sequence<Path> {
    return System.getenv("PATH").orEmpty()
            .splitToSequence(File.pathSeparator)
            .filter { !it.isEmpty() }
            .map { Paths.get(it) }
            .filter { it.isDirectory() }
}

private fun suggestionsForMac(): Sequence<Path> {
    if (!SystemInfo.isMac) return emptySequence()
    return sequenceOf(Paths.get("/usr/local/bin"))
}

private fun suggestionsForUnix(): Sequence<Path> {
    if (!SystemInfo.isUnix) return emptySequence()
    return sequenceOf(Paths.get("/usr/local/bin"))
}

private fun suggestionsForWindows(): Sequence<Path> {
    if (!SystemInfo.isWindows) return emptySequence()
    return sequenceOf(
            Paths.get("C:/Program Files (x86)/Elm Platform/0.19/bin"),
            Paths.get("C:/Program Files/Elm Platform/0.19/bin"),
            Paths.get("C:/Program Files (x86)/Elm Platform/0.18/bin"), // TODO [drop 0.18]
            Paths.get("C:/Program Files/Elm Platform/0.18/bin"))
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
