package org.elm.lang.core.stubs.index

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.stubs.ElmFileStub
import org.elm.lang.core.stubs.ElmModuleDeclarationStub

private val logger = Logger.getInstance(ElmModulesIndex::class.java)

/**
 * Low-level index of all known Elm modules in an IntelliJ project.
 *
 * **IMPORTANT:** For most application code, this is far too general. Typically you would
 * want to restrict the module name space by an Elm project, as defined by an `elm.json` file.
 * In which case you should instead use [ElmModules].
 *
 * @see ElmModules
 */
class ElmModulesIndex : StringStubIndexExtension<ElmModuleDeclaration>() {

    override fun getVersion() =
            ElmFileStub.Type.stubVersion

    override fun getKey(): StubIndexKey<String, ElmModuleDeclaration> =
            KEY

    companion object {
        val KEY: StubIndexKey<String, ElmModuleDeclaration> =
                StubIndexKey.createIndexKey("org.elm.lang.core.stubs.index.ElmModulesIndex")

        fun index(stub: ElmModuleDeclarationStub, indexSink: IndexSink) {
            val key = makeKey(stub.psi)
            indexSink.occurrence(KEY, key)
        }

        private fun makeKey(moduleDeclaration: ElmModuleDeclaration) =
                makeKey(moduleDeclaration.name)

        private fun makeKey(moduleName: String) =
                moduleName

        /**
         * Return the module declaration for [moduleName], if any.
         */
        fun get(moduleName: String, project: Project): ElmModuleDeclaration? {
            val key = makeKey(moduleName)
            val matches = StubIndex.getElements(KEY, key, project,
                    GlobalSearchScope.allScope(project),
                    ElmModuleDeclaration::class.java)
                    .sortedWith(elmAppVsLibraryComparator)

            if (logger.isDebugEnabled && matches.size > 1)
                logger.warn("multiple modules found for $moduleName")

            return matches.firstOrNull()
        }

        /**
         * Returns all module declarations in [project] whose module name
         * matches an item in [moduleNames]
         */
        fun getAll(moduleNames: Collection<String>, project: Project): List<ElmModuleDeclaration> {
            val index = StubIndex.getInstance()
            val results = mutableListOf<ElmModuleDeclaration>()

            for (key in moduleNames) {
                index.processElements(KEY, key, project, GlobalSearchScope.allScope(project),
                        ElmModuleDeclaration::class.java) {
                    results.add(it)
                }
            }
            return results.sortedWith(elmAppVsLibraryComparator)
        }

        /**
         * Returns all module declarations in [project]
         */
        fun getAll(project: Project): List<ElmModuleDeclaration> {
            return getAll(StubIndex.getInstance().getAllKeys(KEY, project), project)
        }
    }
}

/*
There should be only a single file in project scope for a given module name,
but until we start loading the `elm-package.json` manifest file to determine
source roots and library roots, we will need to deal with indexed Elm files
that do not truly belong to THIS project. To mitigate the problem, we will
order the module declarations such that the ones that live in the `elm-stuff`
directory are at the end of the list.
*/
private val elmAppVsLibraryComparator =
        compareBy<ElmModuleDeclaration> {
            if (it.containingFile.virtualFile.path.contains("/elm-stuff/")) 1 else 0
        }
