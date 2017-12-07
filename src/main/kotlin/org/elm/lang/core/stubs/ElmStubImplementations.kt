package org.elm.lang.core.stubs

import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.IStubFileElementType
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.ElmFile


class ElmFileStub(file: ElmFile?): PsiFileStubImpl<ElmFile>(file) {

    override fun getType() = Type


    object Type : IStubFileElementType<ElmFileStub>(ElmLanguage) {

        override fun getStubVersion() = 0

        override fun getBuilder() =
                object : DefaultStubBuilder() {
                    override fun createStubForFile(file: PsiFile) =
                        ElmFileStub(file as ElmFile)
                }

        override fun serialize(stub: ElmFileStub, dataStream: StubOutputStream) {
            // no data to write
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): ElmFileStub {
            // no data to read
            return ElmFileStub(null)
        }

        override fun getExternalId() =
                "Elm.file"
    }
}

fun factory(name: String): ElmStubElementType<*, *> = when (name) {
    // TODO [kl] manually
    else -> error("Unknown element $name")
}
