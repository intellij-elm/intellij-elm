package org.elm.workspace

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.exists
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val log = Logger.getInstance(ElmToolchain::class.java)


data class ElmToolchain(
        val elmCompilerPath: Path?,
        val elmFormatPath: Path?,
        val isElmFormatOnSaveEnabled: Boolean
) {
    constructor(elmCompilerPath: String, elmFormatPath: String, isElmFormatOnSaveEnabled: Boolean) :
            this(
                    if (elmCompilerPath.isNotBlank()) Paths.get(elmCompilerPath) else null,
                    if (elmFormatPath.isNotBlank()) Paths.get(elmFormatPath) else null,
                    isElmFormatOnSaveEnabled
            )

    val elmCLI: ElmCLI? =
            elmCompilerPath?.let { ElmCLI(it) }

    val elmFormatCLI: ElmFormatCLI? =
            elmFormatPath?.let { ElmFormatCLI(it) }

    val presentableLocation: String =
            elmCompilerPath?.toString() ?: "unknown location"

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

    fun looksLikeValidToolchain(): Boolean =
            elmCompilerPath != null && Files.isExecutable(elmCompilerPath)

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

    /**
     * Attempts to locate Elm tool paths for all tools which are un-configured.
     * Returns a copy of the receiver. Performs file I/O.
     */
    fun autoDiscoverAll(project: Project): ElmToolchain {
        val suggestions = ElmSuggest.suggestTools(project)
        return copy(
                elmCompilerPath = elmCompilerPath ?: suggestions["elm"],
                elmFormatPath = elmFormatPath ?: suggestions["elm-format"]
        )
    }

    companion object {
        const val ELM_JSON = "elm.json"
        const val ELM_LEGACY_JSON = "elm-package.json" // TODO [drop 0.18]
        const val DEFAULT_FORMAT_ON_SAVE = false

        /**
         * A blank, default [ElmToolchain].
         */
        val BLANK = ElmToolchain(
                elmCompilerPath = null,
                elmFormatPath = null,
                isElmFormatOnSaveEnabled = ElmToolchain.DEFAULT_FORMAT_ON_SAVE
        )


        // TODO [drop 0.18] this list will no longer be necessary once the migration to 0.19 is complete
        val ELM_MANIFEST_FILE_NAMES = listOf(ElmToolchain.ELM_JSON, ElmToolchain.ELM_LEGACY_JSON)

        // TODO [drop 0.18] set the min compiler version to 0.19
        val MIN_SUPPORTED_COMPILER_VERSION = Version(0, 18, 0)

        /**
         * Suggest a default toolchain based on common locations where Elm tools are frequently installed.
         * This performs file I/O.
         */
        fun suggest(project: Project): ElmToolchain =
                BLANK.autoDiscoverAll(project)
    }
}
