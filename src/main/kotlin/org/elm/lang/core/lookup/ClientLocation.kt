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
 * are only "import-able" from within "$ProjectRoot/tests" directory.
 *
 * @property intellijProject The IntelliJ project
 * @property elmProject The Elm project from which we want to look for something
 * @property isInTestsDirectory True if the place we are searching from is within the "tests" directory
 */
interface ClientLocation {
    val intellijProject: Project
    val elmProject: ElmProject?
    val isInTestsDirectory: Boolean
}
