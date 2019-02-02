package org.elm.lang.core.stubs.index

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.stubs.ElmFileStub

/**
 * An index of all Elm named things across the entire IntelliJ project.
 *
 * IMPORTANT: See [ElmLookup] for an alternative API that properly
 * handles visibility of named things based on the Elm project which
 * wants to access it.
 *
 */
class ElmNamedElementIndex : StringStubIndexExtension<ElmNamedElement>() {

    override fun getVersion() =
            ElmFileStub.Type.stubVersion

    override fun getKey(): StubIndexKey<String, ElmNamedElement> =
            KEY

    companion object {
        val KEY: StubIndexKey<String, ElmNamedElement> =
                StubIndexKey.createIndexKey("org.elm.lang.core.stubs.index.ElmNamedElementIndex")

        /**
         * Find all [ElmNamedElement]s whose name matches [name] in [scope].
         */
        fun find(name: String, project: Project, scope: GlobalSearchScope): Collection<ElmNamedElement> =
                StubIndex.getElements(KEY, name, project, scope, ElmNamedElement::class.java)

        /**
         * Get the name of every element stored in this index.
         */
        fun getAllNames(project: Project): Collection<String> =
                StubIndex.getInstance().getAllKeys(KEY, project)
    }
}
