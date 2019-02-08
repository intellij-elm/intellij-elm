package org.elm.workspace

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.Result
import org.elm.openapiext.modules
import java.io.File
import java.io.IOException
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

    /**
     * Path to directory for a package at a specific version, containing `elm.json`
     */
    fun packageVersionDir(name: String, version: Version): Path? {
        // TODO [kl] stop hard-coding the compiler version
        // it's ok to assume 19 here because this will never be called from 0.18 code,
        // but even this assumption will not be safe once future 19 releases are made.
        val compilerVersion = "0.19.0"

        return Paths.get("$elmHomePath/$compilerVersion/package/$name/$version/")
    }

    /**
     * Path to the manifest file for the Elm package [name] at version [version]
     */
    fun findPackageManifest(name: String, version: Version): Path? {
        // TODO [kl] use compiler version to determine whether to use elm.json vs elm-package.json
        return packageVersionDir(name, version)?.resolve(ELM_JSON)
    }

    /**
     * Path to directory for a package, containing one or more versions
     */
    fun availableVersionsForPackage(name: String): List<Version> {
        // TODO [kl] stop hard-coding the compiler version
        val compilerVersion = "0.19.0"

        val files = File("$elmHomePath/$compilerVersion/package/$name/").listFiles()
                ?: return emptyList()
        return files.mapNotNull { Version.parseOrNull(it.name) }
    }

    fun queryCompilerVersion(): Result<Version> {
        val elm = elmCompilerPath ?: return Result.Err("Elm compiler not found")

        // NodeJS tools like npm and nvm like to play games with wrapper scripts around native binaries
        // such as the Elm compiler. So we will try to detect the wrapper and notify the user.
        // See https://github.com/klazuka/intellij-elm/issues/252
        try {
            val firstChars = ByteArray(2)
            elm.toFile().inputStream().read(firstChars, 0, 2)
            if (firstChars.toString(Charsets.US_ASCII) == "#!") {
                return Result.Err("path points to a wrapper script; please use the path to the actual Elm compiler")
            }
        } catch (e: IOException) {
            return Result.Err("could not peek at the first bytes in the Elm compiler")
        }

        // Output of `elm --version` is a single line containing the version number (e.g. `0.19.0\n`)
        val versionString = GeneralCommandLine(elm).withParameters("--version").runExecutable()?.firstOrNull()
                ?: return Result.Err("failed to run the Elm compiler")

        return try {
            Result.Ok(Version.parse(versionString))
        } catch (e: ParseException) {
            Result.Err("invalid Elm version: ${e.message}")
        }
    }

    companion object {
        const val ELM_JSON = "elm.json"
        const val ELM_LEGACY_JSON = "elm-package.json" // TODO [drop 0.18]

        // TODO [drop 0.18] this list will no longer be necessary once the migration to 0.19 is complete
        val ELM_MANIFEST_FILE_NAMES = listOf(ElmToolchain.ELM_JSON, ElmToolchain.ELM_LEGACY_JSON)

        // TODO [drop 0.18] set the min compiler version to 0.19
        val MIN_SUPPORTED_COMPILER_VERSION = Version(0, 18, 0)

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
                suggestionsForUnix(),
                suggestionsFromNVM()
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

private fun suggestionsFromNVM(): Sequence<Path> {
    // nvm (Node Version Manager): see https://github.com/klazuka/intellij-elm/issues/252
    // nvm is not available on Windows
    if (SystemInfo.isWindows) return emptySequence()
    return sequenceOf(Paths.get(FileUtil.expandUserHome("~/.config/yarn/global/node_modules/elm/unpacked_bin")))
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
            Paths.get("C:/Program Files (x86)/Elm Platform/0.19/bin"), // npm install -g elm
            Paths.get("C:/Program Files/Elm Platform/0.19/bin"),
            Paths.get("C:/Program Files (x86)/Elm/0.19/bin"), // choco install elm-platform
            Paths.get("C:/Program Files/Elm/0.19/bin"),
            Paths.get("C:/Program Files (x86)/Elm Platform/0.18/bin"), // TODO [drop 0.18]
            Paths.get("C:/Program Files/Elm Platform/0.18/bin"),
            Paths.get("C:/Program Files (x86)/Elm/0.18/bin"), // TODO [drop 0.18]
            Paths.get("C:/Program Files/Elm/0.18/bin")
    )
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
