package org.elm.workspace

import com.intellij.execution.ExecutionException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import com.intellij.util.io.isDirectory
import org.elm.openapiext.GeneralCommandLine
import org.elm.openapiext.Result
import org.elm.openapiext.execute
import org.elm.openapiext.modules
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val log = Logger.getInstance(ElmToolchain::class.java)


data class ElmToolchain(val binDirPath: Path, val isElmFormatOnSaveEnabled: Boolean) {
    constructor(binDirPath: String, isElmFormatOnSaveEnabled: Boolean) : this(Paths.get(binDirPath), isElmFormatOnSaveEnabled)

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
        return executableNameSuggestionsFor("elm")
                .map { binDirPath.resolve(it) }
                .firstOrNull { Files.isExecutable(it) }
    }

    val elmTestPath: Path? get() {
        return executableNameSuggestionsFor("elm-test")
                .map { binDirPath.resolve(it) }
                .firstOrNull { Files.isExecutable(it) }
    }
    val elmFormat: ElmFormatCLI?
        get() = executableNameSuggestionsFor("elm-format")
                .map { binDirPath.resolve(it) }
                .firstOrNull { Files.isExecutable(it) }
                ?.let { ElmFormatCLI(it) }

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

        // Output of `elm --version` is a single line containing the version number (e.g. `0.19.0\n`)
        val versionString = try {
            GeneralCommandLine(elm).withParameters("--version")
                    .execute(timeoutInMilliseconds = 1500)
                    .stdoutLines
                    .firstOrNull()
        } catch (e: ExecutionException) {
            log.debug("Failed to run `elm --version`", e)
            null
        }
        if (versionString == null) {
            // NodeJS tools like npm and nvm like to play games with wrapper scripts around native binaries
            // such as the Elm compiler. So we will try to detect if the wrapper may have been the problem
            // and notify the user. See https://github.com/klazuka/intellij-elm/issues/252
            return if (elm.toFile().isWrapperScript()) {
                Result.Err("the 'elm' file here is a wrapper script; please use the path to the actual Elm compiler")
            } else {
                Result.Err("failed to run the Elm compiler")
            }
        }

        return try {
            Result.Ok(Version.parse(versionString))
        } catch (e: ParseException) {
            Result.Err("invalid Elm version: ${e.message}")
        }
    }

    fun queryElmFormatVersion(): Result<Version> {
        val elmFormat = elmFormat ?: return Result.Err("elm-format not found")

        // Output of `elm-format` is multiple lines where the first line is 'elm-format 0.8.1'

        val elmFormatRegex = Regex("elm-format (\\d+(?:\\.\\d+){2})")

        val versionString = try {
            GeneralCommandLine(elmFormat.elmFormatExecutablePath)
                    .execute(timeoutInMilliseconds = 1500)
                    .stdoutLines
                    .firstOrNull()
        } catch (e: ExecutionException) {
            log.debug("Failed to run `elm-format`", e)
            null
        }
        if (versionString == null) {
            // NodeJS tools like npm and nvm like to play games with wrapper scripts around native binaries
            // such as the Elm compiler. So we will try to detect if the wrapper may have been the problem
            // and notify the user. See https://github.com/klazuka/intellij-elm/issues/252
            return if (elmFormat.elmFormatExecutablePath.toFile().isWrapperScript()) {
                Result.Err("the 'elm-format' file here is a wrapper script; please use the path to the actual executable")
            } else {
                Result.Err("failed to run the elm-format")
            }
        }

        return try {
            val matchResult = elmFormatRegex.matchEntire(versionString)

            if(matchResult == null) return Result.Err("invalid elm-format version string: ${versionString}")

            val (elmVersionString) = matchResult.destructured
            Result.Ok(Version.parse(elmVersionString))
        } catch (e: ParseException) {
            Result.Err("invalid elm-format version: ${e.message}")
        }
    }

    companion object {
        const val ELM_JSON = "elm.json"
        const val ELM_LEGACY_JSON = "elm-package.json" // TODO [drop 0.18]
        const val DEFAULT_FORMAT_ON_SAVE = false

        // TODO [drop 0.18] this list will no longer be necessary once the migration to 0.19 is complete
        val ELM_MANIFEST_FILE_NAMES = listOf(ElmToolchain.ELM_JSON, ElmToolchain.ELM_LEGACY_JSON)

        // TODO [drop 0.18] set the min compiler version to 0.19
        val MIN_SUPPORTED_COMPILER_VERSION = Version(0, 18, 0)

        /** Suggest a toolchain that exists in in any standard location */
        fun suggest(project: Project): ElmToolchain? {
            return binDirSuggestions(project)
                    .map { ElmToolchain(it.toAbsolutePath(), DEFAULT_FORMAT_ON_SAVE) }
                    .firstOrNull { it.looksLikeValidToolchain() }
        }
    }
}

// Look for both installed and npm versions of the binary
private fun executableNameSuggestionsFor(name: String) =
        if (SystemInfo.isWindows) sequenceOf("$name.exe", "$name.cmd", name)
        else sequenceOf(name)

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


/** Returns true if the file looks like a wrapper script created by NodeJS tools like npm */
private fun File.isWrapperScript(): Boolean {
    val firstChars = try {
        val bytes = ByteArray(3)
        inputStream().use { it.read(bytes, 0, 3) }
        bytes.toString(Charsets.US_ASCII)
    } catch (e: IOException) {
        return false
    }

    return when (firstChars) {
        "#!/" -> true // hash-bang used by Unix scripts
        "@IF" -> true // npm Windows batch script starts with `@IF EXIST "%~dp0\node.exe"`
        else -> false
    }
}
