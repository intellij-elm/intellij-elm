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
         * Returns all module declarations with name [moduleName]
         */
        fun get(moduleName: String, project: Project): List<ElmModuleDeclaration> {
            val key = makeKey(moduleName)
            return StubIndex.getElements(KEY, key, project,
                    GlobalSearchScope.allScope(project),
                    ElmModuleDeclaration::class.java).toList()
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
            return results
        }

        /**
         * Returns all module declarations in [project]
         */
        fun getAll(project: Project): List<ElmModuleDeclaration> {
            return getAll(StubIndex.getInstance().getAllKeys(KEY, project), project)
        }
    }
}
