package org.elm.workspace

import com.intellij.openapi.project.Project
import org.elm.workspace.commandLineTools.ElmCLI
import org.elm.workspace.commandLineTools.ElmFormatCLI
import org.elm.workspace.commandLineTools.ElmTestCLI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class ElmToolchain(
        val elmCompilerPath: Path?,
        val elmFormatPath: Path?,
        val elmTestPath: Path?,
        val isElmFormatOnSaveEnabled: Boolean
) {
    constructor(elmCompilerPath: String, elmFormatPath: String, elmTestPath: String, isElmFormatOnSaveEnabled: Boolean) :
            this(
                    if (elmCompilerPath.isNotBlank()) Paths.get(elmCompilerPath) else null,
                    if (elmFormatPath.isNotBlank()) Paths.get(elmFormatPath) else null,
                    if (elmTestPath.isNotBlank()) Paths.get(elmTestPath) else null,
                    isElmFormatOnSaveEnabled
            )

    val elmCLI: ElmCLI? = elmCompilerPath?.let { ElmCLI(it) }

    val elmFormatCLI: ElmFormatCLI? = elmFormatPath?.let { ElmFormatCLI(it) }

    val elmTestCLI: ElmTestCLI? = elmTestPath?.let { ElmTestCLI(it) }

    val presentableLocation: String =
            elmCompilerPath?.toString() ?: "unknown location"

    fun looksLikeValidToolchain(): Boolean =
            elmCompilerPath != null && Files.isExecutable(elmCompilerPath)

    /**
     * Attempts to locate Elm tool paths for all tools which are un-configured.
     * Returns a copy of the receiver. Performs file I/O.
     */
    fun autoDiscoverAll(project: Project): ElmToolchain {
        val suggestions = ElmSuggest.suggestTools(project)
        return copy(
                elmCompilerPath = elmCompilerPath ?: suggestions["elm"],
                elmFormatPath = elmFormatPath ?: suggestions["elm-format"],
                elmTestPath = elmTestPath ?: suggestions["elm-test"]
        )
    }

    companion object {
        const val ELM_JSON = "elm.json"

        /**
         * The name of the file that contains information specific to an Elm project, but which is _not_ in `elm.json`.
         * Here we put extra information which isn't in the normal `elm.json`, but which this plugin requires, such as
         * a custom path to the directory containing tests.
         *
         * The `elm.json` file is referred to elsewhere as the _manifest_. This `elm.intellij.json` file is referred to
         * as the _sidecar manifest_.
         */
        const val SIDECAR_FILENAME = "elm.intellij.json"

        const val DEFAULT_FORMAT_ON_SAVE = false

        /**
         * A blank, default [ElmToolchain].
         */
        val BLANK = ElmToolchain(
                elmCompilerPath = null,
                elmFormatPath = null,
                elmTestPath = null,
                isElmFormatOnSaveEnabled = ElmToolchain.DEFAULT_FORMAT_ON_SAVE
        )

        val MIN_SUPPORTED_COMPILER_VERSION = Version(0, 19, 0)

        /**
         * Suggest a default toolchain based on common locations where Elm tools are frequently installed.
         * This performs file I/O.
         */
        fun suggest(project: Project): ElmToolchain =
                BLANK.autoDiscoverAll(project)
    }
}
