package org.elm.lang.core.stubs

import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.DefaultStubBuilder
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.PsiFileStubImpl
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.IStubFileElementType
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.ElmModuleDeclaration


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
    // TODO [kl] manually map stubbable element names to type impls
    "MODULE_DECLARATION" -> ElmModuleDeclarationStub.Type
    else -> error("Unknown element $name")
}

class ElmModuleDeclarationStub(parent: StubElement<*>?,
                               elementType: IStubElementType<*, *>,
                               override val name: String?
): StubBase<ElmModuleDeclaration>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmModuleDeclarationStub, ElmModuleDeclaration>("MODULE_DECLARATION") {

        override fun serialize(stub: ElmModuleDeclarationStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmModuleDeclarationStub(parentStub, this,
                        dataStream.readNameAsString())

        override fun createPsi(stub: ElmModuleDeclarationStub) =
                ElmModuleDeclaration(stub, this)

        override fun createStub(psi: ElmModuleDeclaration, parentStub: StubElement<*>?) =
                ElmModuleDeclarationStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmModuleDeclarationStub, sink: IndexSink) {
            // TODO [kl] index me
        }
    }
}


private fun StubInputStream.readNameAsString(): String? = readName()?.string
private fun StubInputStream.readUTFFastAsNullable(): String? = readUTFFast().let { if (it == "") null else it }

private fun StubOutputStream.writeUTFFastAsNullable(value: String?) = writeUTFFast(value ?: "")