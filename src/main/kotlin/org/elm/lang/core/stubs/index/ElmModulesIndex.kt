package org.elm.lang.core.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.elm.lang.core.lookup.ClientLocation
import org.elm.lang.core.lookup.ElmLookup
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.stubs.ElmFileStub
import org.elm.lang.core.stubs.ElmModuleDeclarationStub
import org.elm.workspace.ElmProject

/**
 * Find Elm modules within an Elm project.
 *
 * This API takes into account [ElmProject] structure to determine which modules
 * should be visible, resolving each module to the correct version for the project.
 */
class ElmModulesIndex : StringStubIndexExtension<ElmModuleDeclaration>() {

    override fun getVersion() =
            ElmFileStub.Type.stubVersion

    override fun getKey(): StubIndexKey<String, ElmModuleDeclaration> =
            KEY

    companion object {

        /**
         * Returns an Elm module named [moduleName] which is visible to [clientLocation], if any
         */
        fun get(moduleName: String, clientLocation: ClientLocation): ElmModuleDeclaration? =
                rawGet(moduleName, clientLocation.intellijProject, ElmLookup.searchScopeAt(clientLocation))
                        .firstOrNull()


        /**
         * Returns all Elm modules which are visible to [clientLocation]
         */
        fun getAll(clientLocation: ClientLocation): List<ElmModuleDeclaration> =
                rawGetAll(clientLocation.intellijProject, ElmLookup.searchScopeAt(clientLocation))


        /**
         * Returns all Elm modules whose names match an element in [moduleNames] and which are visible to [clientLocation]
         */
        fun getAll(moduleNames: Collection<String>, clientLocation: ClientLocation): List<ElmModuleDeclaration> =
                rawGetAll(moduleNames, clientLocation.intellijProject, ElmLookup.searchScopeAt(clientLocation))


        // INTERNALS


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
         * Returns all module declarations within [scope] with name [moduleName]
         */
        private fun rawGet(moduleName: String, project: Project, scope: GlobalSearchScope): List<ElmModuleDeclaration> {
            val key = makeKey(moduleName)
            return StubIndex.getElements(KEY, key, project, scope, ElmModuleDeclaration::class.java).toList()
        }


        /**
         * Returns all module declarations within [scope] whose module name matches an item in [moduleNames]
         */
        private fun rawGetAll(moduleNames: Collection<String>, project: Project, scope: GlobalSearchScope): List<ElmModuleDeclaration> {
            val index = StubIndex.getInstance()
            val results = mutableListOf<ElmModuleDeclaration>()

            for (key in moduleNames) {
                index.processElements(KEY, key, project, scope, ElmModuleDeclaration::class.java) {
                    results.add(it)
                }
            }
            return results
        }


        /**
         * Returns all module declarations within [scope]
         */
        private fun rawGetAll(project: Project, scope: GlobalSearchScope): List<ElmModuleDeclaration> =
                rawGetAll(StubIndex.getInstance().getAllKeys(KEY, project), project, scope)

    }
}
