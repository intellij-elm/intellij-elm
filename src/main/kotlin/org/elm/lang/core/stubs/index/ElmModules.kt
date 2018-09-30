package org.elm.lang.core.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.openapiext.findFileByMaybeRelativePath
import org.elm.openapiext.findFileByPath
import org.elm.workspace.ElmProject


// TODO [kl] figure out a better name for this. Maybe it should be part of the 'Scope' system?

/**
 * Find Elm modules within an Elm project.
 */
class ElmModules {
    companion object {

        /**
         * Returns all Elm modules which are visible to [elmProject]
         */
        fun getAll(intellijProject: Project, elmProject: ElmProject?) =
                ElmModulesIndex.getAll(intellijProject)
                        .filter { elmProject.exposes(it) }

        /**
         * Returns all Elm modules whose names match an element in [moduleNames] and which are visible to [elmProject]
         */
        fun getAll(moduleNames: Collection<String>, intellijProject: Project, elmProject: ElmProject?) =
                ElmModulesIndex.getAll(moduleNames, intellijProject)
                        .filter { elmProject.exposes(it) }

        /**
         * Returns an Elm module named [moduleName] which is visible to [elmProject], if any
         */
        fun get(moduleName: String, intellijProject: Project, elmProject: ElmProject?) =
                ElmModulesIndex.get(moduleName, intellijProject)
                        ?.takeIf { elmProject.exposes(it) }
    }
}


/**
 * Returns true if [moduleDeclaration] is visible within the receiver [ElmProject].
 */
private fun ElmProject?.exposes(moduleDeclaration: ElmModuleDeclaration): Boolean {
    if (this == null) {
        // The lightweight integration tests do not have an associated Elm Project,
        // so we will just treat them as if all Elm files were in scope.
        //
        // TODO [kl] re-visit this as it could mask a real bug.
        return true
    }

    val elmModuleRelativePath = moduleDeclaration.name.replace('.', '/') + ".elm"

    // check if the module is reachable from the top-level of the containing Elm project
    val virtualDirs = sourceDirectories.map { projectDirPath.resolve(it) }
            .mapNotNull { LocalFileSystem.getInstance().findFileByPath(it) }
    if (virtualDirs.any { it.findFileByMaybeRelativePath(elmModuleRelativePath) != null })
        return true

    // check if the module is reachable from a direct dependency
    if (dependencies.any { it.exposedModules.contains(moduleDeclaration.name) })
        return true

    return false
}