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
        fun getAll(intellijProject: Project, elmProject: ElmProject?): List<ElmModuleDeclaration> {
            // TODO [kl] make more restrictive by forbidding null [ElmProject] arg
            val allModules = ElmModulesIndex.getAll(intellijProject)
            return when (elmProject) {
                null -> allModules
                else -> allModules.filter { elmProject.exposes(it) }
            }
        }

        /**
         * Returns all Elm modules whose names match an element in [moduleNames] and which are visible to [elmProject]
         */
        fun getAll(moduleNames: Collection<String>, intellijProject: Project, elmProject: ElmProject?): List<ElmModuleDeclaration> {
            // TODO [kl] make more restrictive by forbidding null [ElmProject] arg
            val allModules = ElmModulesIndex.getAll(moduleNames, intellijProject)
            return when (elmProject) {
                null -> allModules
                else -> allModules.filter { elmProject.exposes(it) }
            }
        }

        /**
         * Returns an Elm module named [moduleName] which is visible to [elmProject], if any
         */
        fun get(moduleName: String, intellijProject: Project, elmProject: ElmProject?): ElmModuleDeclaration? {
            // TODO [kl] make more restrictive by forbidding null [ElmProject] arg
            val elmModule = ElmModulesIndex.get(moduleName, intellijProject)
            return when (elmProject) {
                null -> elmModule
                else -> elmModule?.takeIf { elmProject.exposes(it) }
            }
        }
    }
}


/**
 * Returns true if [moduleDeclaration] is visible within the receiver [ElmProject].
 */
private fun ElmProject.exposes(moduleDeclaration: ElmModuleDeclaration): Boolean {
    if (moduleDeclaration.elmProject?.manifestPath == manifestPath) {
        // The module declaration belongs to *this* Elm project.
        // Check if the module is reachable from this project's source directories.
        return sourceDirectoryContains(moduleDeclaration)
    } else {
        // The module declaration belongs to a *different* Elm project.
        // Check if the module is reachable from this project's dependencies
        return dependencies.asSequence()
                .filter { it.exposedModules.contains(moduleDeclaration.name) }
                .any { it.sourceDirectoryContains(moduleDeclaration) }
    }

}


/**
 * Returns true if [moduleDeclaration] can be found in the receiver's source directories.
 */
private fun ElmProject.sourceDirectoryContains(moduleDeclaration: ElmModuleDeclaration): Boolean {
    if (moduleDeclaration.elmProject?.manifestPath != manifestPath) {
        // must belong to the same Elm project!
        return false
    }

    val elmModuleRelativePath = moduleDeclaration.name.replace('.', '/') + ".elm"
    return sourceDirectories.asSequence()
            .map { projectDirPath.resolve(it) }
            .mapNotNull { LocalFileSystem.getInstance().findFileByPath(it) }
            .any { it.findFileByMaybeRelativePath(elmModuleRelativePath) != null }
}