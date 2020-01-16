package org.elm.lang.core.lookup

import com.intellij.openapi.project.Project
import org.elm.workspace.ElmProject

/**
 * Describes the location from which a reference is resolved.
 *
 * Reference resolution depends on context. For instance, we need
 * to know the containing Elm project in order to determine which
 * `source-directories` are valid roots.
 *
 * Starting in Elm 0.19, the Elm project's `test-dependencies`
 * are normally "import-able" from within "$ProjectRoot/tests" directory,
 * although a different path can be configured. See [ElmProject.testsDirPath].
 *
 * @property intellijProject The IntelliJ project
 * @property elmProject The Elm project from which we want to look for something
 * @property isInTestsDirectory True if the place we are searching from is within the directory containing unit tests.
 */
interface ClientLocation {
    val intellijProject: Project
    val elmProject: ElmProject?
    val isInTestsDirectory: Boolean
}
