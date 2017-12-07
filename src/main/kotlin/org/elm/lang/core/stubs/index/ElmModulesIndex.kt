package org.elm.lang.core.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.elm.lang.core.psi.elements.ElmModuleDeclaration
import org.elm.lang.core.stubs.ElmFileStub
import org.elm.lang.core.stubs.ElmModuleDeclarationStub


class ElmModulesIndex: StringStubIndexExtension<ElmModuleDeclaration>() {

    override fun getVersion() =
            ElmFileStub.Type.stubVersion

    override fun getKey(): StubIndexKey<String, ElmModuleDeclaration> =
            KEY

    companion object {
        val KEY: StubIndexKey<String, ElmModuleDeclaration> =
                StubIndexKey.createIndexKey("org.elm.lang.core.stubs.index.ElmModulesIndex")

        fun index(stub: ElmModuleDeclarationStub, indexSink: IndexSink) {
            val key = makeKey(stub.psi)
            if (key != null) {
                indexSink.occurrence(KEY, key)
            }
        }

        private fun makeKey(moduleDeclaration: ElmModuleDeclaration): String? {
            // TODO [kl] what about the Cmd and Sub module name hacks?
            return moduleDeclaration.name
        }

        fun get(moduleName: String, project: Project): ElmModuleDeclaration? {
            val matches = StubIndex.getElements(KEY, moduleName, project,
                    GlobalSearchScope.allScope(project),
                    ElmModuleDeclaration::class.java)

            // TODO [kl] prioritize the results
            return matches.firstOrNull()
        }

        fun getAll(project: Project): List<ElmModuleDeclaration> {
            val index = StubIndex.getInstance()
            val results = mutableListOf<ElmModuleDeclaration>()

            for (key in index.getAllKeys(KEY, project)) {
                index.processElements(KEY, key, project, GlobalSearchScope.allScope(project),
                        ElmModuleDeclaration::class.java) {
                    results.add(it)
                }
            }
            // TODO [kl] prioritize the results
            return results
        }
    }
}
