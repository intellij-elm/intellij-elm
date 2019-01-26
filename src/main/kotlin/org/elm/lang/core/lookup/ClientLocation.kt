package org.elm.lang.core.lookup

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.openapiext.findFileByPathTestAware
import org.elm.workspace.ElmPackageProject
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


    /**
     * Returns `true` if [moduleDeclaration] can be seen from [this location][ClientLocation].
     *
     * @see [searchScope]
     */
    fun canSee(moduleDeclaration: ElmModuleDeclaration): Boolean =
            searchScope().contains(moduleDeclaration.elmFile.originalFile.virtualFile)


    /**
     * Returns a [GlobalSearchScope] which includes all Elm files that belong to [elmProject]
     * taking into consideration:
     *
     *  - which source roots the [ElmProject] defines
     *  - which packages are visible from the client location's [ElmProject]
     *  - whether "test" code (and dependencies) should be included
     */
    fun searchScope(): GlobalSearchScope {
        val p = elmProject ?: return GlobalSearchScope.EMPTY_SCOPE

        val sourceDirs = p.sourceDirsVisibleAt(this).mapNotNull { findFileByPathTestAware(it) }.toList()
        val srcDirScope = GlobalSearchScopesCore.directoriesScope(intellijProject, true, *(sourceDirs.toTypedArray()))

        val exposedDependencyFiles = p.dependenciesVisibleAt(this).flatMap { it.exposedFiles() }.toList()
        val dependenciesScope = GlobalSearchScope.filesWithLibrariesScope(intellijProject, exposedDependencyFiles)

        return srcDirScope.uniteWith(dependenciesScope)
    }
}

/**
 * Return the [VirtualFile] for each Elm module which is exposed by this package.
 */
private fun ElmPackageProject.exposedFiles(): Sequence<VirtualFile> =
        exposedModules.mapNotNull { moduleName ->
            val elmModuleRelativePath = moduleName.replace('.', '/') + ".elm"
            absoluteSourceDirectories.mapNotNull { srcDirPath ->
                findFileByPathTestAware(srcDirPath.resolve(elmModuleRelativePath))
            }.firstOrNull()
        }.asSequence()