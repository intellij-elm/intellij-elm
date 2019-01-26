package org.elm

import com.intellij.openapi.project.Project
import org.elm.lang.core.lookup.ClientLocation
import org.elm.workspace.ElmProject

/**
 * A [ClientLocation] for testing purposes
 */
data class TestClientLocation(
        override val intellijProject: Project,
        override val elmProject: ElmProject?,
        override val isInTestsDirectory: Boolean = false
) : ClientLocation