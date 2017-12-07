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
import org.elm.lang.core.psi.elements.ElmTypeAliasDeclaration
import org.elm.lang.core.psi.elements.ElmTypeDeclaration


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
    "MODULE_DECLARATION" -> ElmModuleDeclarationStub.Type
    "TYPE_DECLARATION" -> ElmTypeDeclarationStub.Type
    "TYPE_ALIAS_DECLARATION" -> ElmTypeAliasDeclarationStub.Type
    else -> error("Unknown element $name")
}


class ElmModuleDeclarationStub(parent: StubElement<*>?,
                               elementType: IStubElementType<*, *>,
                               override val name: String
): StubBase<ElmModuleDeclaration>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmModuleDeclarationStub, ElmModuleDeclaration>("MODULE_DECLARATION") {

        override fun serialize(stub: ElmModuleDeclarationStub, dataStream: StubOutputStream) =
            with(dataStream) {
                writeName(stub.name)
            }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmModuleDeclarationStub(parentStub, this,
                        dataStream.readNameAsString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmModuleDeclarationStub) =
                ElmModuleDeclaration(stub, this)

        override fun createStub(psi: ElmModuleDeclaration, parentStub: StubElement<*>?) =
                ElmModuleDeclarationStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmModuleDeclarationStub, sink: IndexSink) {
            // TODO [kl] index me
        }
    }
}


class ElmTypeDeclarationStub(parent: StubElement<*>?,
                               elementType: IStubElementType<*, *>,
                               override val name: String
): StubBase<ElmTypeDeclaration>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmTypeDeclarationStub, ElmTypeDeclaration>("TYPE_DECLARATION") {

        override fun serialize(stub: ElmTypeDeclarationStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmTypeDeclarationStub(parentStub, this,
                        dataStream.readNameAsString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmTypeDeclarationStub) =
                ElmTypeDeclaration(stub, this)

        override fun createStub(psi: ElmTypeDeclaration, parentStub: StubElement<*>?) =
                ElmTypeDeclarationStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmTypeDeclarationStub, sink: IndexSink) {
            // TODO [kl] index me
        }
    }
}


class ElmTypeAliasDeclarationStub(parent: StubElement<*>?,
                               elementType: IStubElementType<*, *>,
                               override val name: String
): StubBase<ElmTypeAliasDeclaration>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmTypeAliasDeclarationStub, ElmTypeAliasDeclaration>("TYPE_ALIAS_DECLARATION") {

        override fun serialize(stub: ElmTypeAliasDeclarationStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmTypeAliasDeclarationStub(parentStub, this,
                        dataStream.readNameAsString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmTypeAliasDeclarationStub) =
                ElmTypeAliasDeclaration(stub, this)

        override fun createStub(psi: ElmTypeAliasDeclaration, parentStub: StubElement<*>?) =
                ElmTypeAliasDeclarationStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmTypeAliasDeclarationStub, sink: IndexSink) {
            // TODO [kl] index me
        }
    }
}


private fun StubInputStream.readNameAsString(): String? = readName()?.string
private fun StubInputStream.readUTFFastAsNullable(): String? = readUTFFast().let { if (it == "") null else it }

private fun StubOutputStream.writeUTFFastAsNullable(value: String?) = writeUTFFast(value ?: "")