package org.elm.lang.core.stubs.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import org.elm.lang.core.psi.ElmNamedElement
import org.elm.lang.core.stubs.ElmFileStub

class ElmNamedElementIndex: StringStubIndexExtension<ElmNamedElement>() {

    override fun getVersion() =
            ElmFileStub.Type.stubVersion

    override fun getKey(): StubIndexKey<String, ElmNamedElement> =
            KEY

    companion object {
        val KEY: StubIndexKey<String, ElmNamedElement> =
                StubIndexKey.createIndexKey("org.elm.lang.core.stubs.index.ElmNamedElementIndex")
    }
}