package org.elm.lang.core.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.*


class ElmFileStub(file: ElmFile?) : PsiFileStubImpl<ElmFile>(file) {

    override fun getType() = Type


    object Type : IStubFileElementType<ElmFileStub>(ElmLanguage) {

        override fun getStubVersion() = 4

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
    "UNION_MEMBER" -> ElmUnionMemberStub.Type
    "FUNCTION_DECLARATION_LEFT" -> ElmFunctionDeclarationLeftStub.Type
    "OPERATOR_DECLARATION_LEFT" -> ElmOperatorDeclarationLeftStub.Type  // TODO [drop 0.18] remove this line
    "INFIX_DECLARATION" -> ElmInfixDeclarationStub.Type
    "EXPOSING_LIST" -> ElmExposingListStub.Type
    "EXPOSED_OPERATOR" -> ElmExposedOperatorStub.Type
    "EXPOSED_VALUE" -> ElmExposedValueStub.Type
    "EXPOSED_TYPE" -> ElmExposedTypeStub.Type
    "EXPOSED_UNION_CONSTRUCTOR" -> ElmExposedUnionConstructorStub.Type
    "EXPOSED_UNION_CONSTRUCTORS" -> ElmExposedUnionConstructorsStub.Type
    "VALUE_DECLARATION" -> ElmValueDeclarationStub.Type
    "PORT_ANNOTATION" -> ElmPortAnnotationStub.Type
    else -> error("Unknown element $name")
}


class ElmModuleDeclarationStub(parent: StubElement<*>?,
                               elementType: IStubElementType<*, *>,
                               override val name: String,
                               val exposesAll: Boolean
) : StubBase<ElmModuleDeclaration>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmModuleDeclarationStub, ElmModuleDeclaration>("MODULE_DECLARATION") {

        override fun serialize(stub: ElmModuleDeclarationStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                    writeBoolean(stub.exposesAll)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmModuleDeclarationStub(parentStub, this,
                        dataStream.readNameAsString() ?: error("expected non-null string"),
                        dataStream.readBoolean())

        override fun createPsi(stub: ElmModuleDeclarationStub) =
                ElmModuleDeclaration(stub, this)

        override fun createStub(psi: ElmModuleDeclaration, parentStub: StubElement<*>?) =
                ElmModuleDeclarationStub(parentStub, this, psi.name, psi.exposesAll)

        override fun indexStub(stub: ElmModuleDeclarationStub, sink: IndexSink) {
            sink.indexModuleDecl(stub)
        }
    }
}


class ElmTypeDeclarationStub(parent: StubElement<*>?,
                             elementType: IStubElementType<*, *>,
                             override val name: String
) : StubBase<ElmTypeDeclaration>(parent, elementType), ElmNamedStub {

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
            sink.indexTypeDecl(stub)
        }
    }
}


class ElmTypeAliasDeclarationStub(parent: StubElement<*>?,
                                  elementType: IStubElementType<*, *>,
                                  override val name: String,
                                  val isRecordAlias: Boolean
) : StubBase<ElmTypeAliasDeclaration>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmTypeAliasDeclarationStub, ElmTypeAliasDeclaration>("TYPE_ALIAS_DECLARATION") {

        override fun serialize(stub: ElmTypeAliasDeclarationStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                    writeBoolean(stub.isRecordAlias)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmTypeAliasDeclarationStub(parentStub, this,
                        dataStream.readNameAsString() ?: error("expected non-null string"),
                        dataStream.readBoolean())

        override fun createPsi(stub: ElmTypeAliasDeclarationStub) =
                ElmTypeAliasDeclaration(stub, this)

        override fun createStub(psi: ElmTypeAliasDeclaration, parentStub: StubElement<*>?) =
                ElmTypeAliasDeclarationStub(parentStub, this, psi.name, psi.isRecordAlias)

        override fun indexStub(stub: ElmTypeAliasDeclarationStub, sink: IndexSink) {
            sink.indexTypeAliasDecl(stub)
        }
    }
}


class ElmUnionMemberStub(parent: StubElement<*>?,
                         elementType: IStubElementType<*, *>,
                         override val name: String
) : StubBase<ElmUnionMember>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmUnionMemberStub, ElmUnionMember>("UNION_MEMBER") {

        override fun serialize(stub: ElmUnionMemberStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmUnionMemberStub(parentStub, this,
                        dataStream.readNameAsString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmUnionMemberStub) =
                ElmUnionMember(stub, this)

        override fun createStub(psi: ElmUnionMember, parentStub: StubElement<*>?) =
                ElmUnionMemberStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmUnionMemberStub, sink: IndexSink) {
            sink.indexUnionMember(stub)
        }
    }
}

class ElmFunctionDeclarationLeftStub(parent: StubElement<*>?,
                                     elementType: IStubElementType<*, *>,
                                     override val name: String
) : StubBase<ElmFunctionDeclarationLeft>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmFunctionDeclarationLeftStub, ElmFunctionDeclarationLeft>("FUNCTION_DECLARATION_LEFT") {

        override fun serialize(stub: ElmFunctionDeclarationLeftStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmFunctionDeclarationLeftStub(parentStub, this,
                        dataStream.readNameAsString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmFunctionDeclarationLeftStub) =
                ElmFunctionDeclarationLeft(stub, this)

        override fun createStub(psi: ElmFunctionDeclarationLeft, parentStub: StubElement<*>?) =
                ElmFunctionDeclarationLeftStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmFunctionDeclarationLeftStub, sink: IndexSink) {
            sink.indexFuncDecl(stub)
        }
    }
}

// TODO [drop 0.18] remove this class
class ElmOperatorDeclarationLeftStub(parent: StubElement<*>?,
                                     elementType: IStubElementType<*, *>,
                                     override val name: String
) : StubBase<ElmOperatorDeclarationLeft>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmOperatorDeclarationLeftStub, ElmOperatorDeclarationLeft>("OPERATOR_DECLARATION_LEFT") {

        override fun serialize(stub: ElmOperatorDeclarationLeftStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmOperatorDeclarationLeftStub(parentStub, this,
                        dataStream.readNameAsString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmOperatorDeclarationLeftStub) =
                ElmOperatorDeclarationLeft(stub, this)

        override fun createStub(psi: ElmOperatorDeclarationLeft, parentStub: StubElement<*>?) =
                ElmOperatorDeclarationLeftStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmOperatorDeclarationLeftStub, sink: IndexSink) {
            sink.indexOperatorDecl(stub)
        }
    }
}



class ElmInfixDeclarationStub(parent: StubElement<*>?,
                              elementType: IStubElementType<*, *>,
                              override val name: String
) : StubBase<ElmInfixDeclaration>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmInfixDeclarationStub, ElmInfixDeclaration>("INFIX_DECLARATION") {

        override fun serialize(stub: ElmInfixDeclarationStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmInfixDeclarationStub(parentStub, this,
                        dataStream.readNameAsString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmInfixDeclarationStub) =
                ElmInfixDeclaration(stub, this)

        override fun createStub(psi: ElmInfixDeclaration, parentStub: StubElement<*>?) =
                ElmInfixDeclarationStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmInfixDeclarationStub, sink: IndexSink) {
            sink.indexInfixDecl(stub)
        }
    }
}


class ElmExposingListStub(parent: StubElement<*>?,
                          elementType: IStubElementType<*, *>
) : StubBase<ElmExposingList>(parent, elementType) {

    object Type : ElmStubElementType<ElmExposingListStub, ElmExposingList>("EXPOSING_LIST") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmExposingListStub, dataStream: StubOutputStream) {
            // nothing to write
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmExposingListStub(parentStub, this)

        override fun createPsi(stub: ElmExposingListStub) =
                ElmExposingList(stub, this)

        override fun createStub(psi: ElmExposingList, parentStub: StubElement<*>?) =
                ElmExposingListStub(parentStub, this)

        override fun indexStub(stub: ElmExposingListStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmExposedValueStub(parent: StubElement<*>?,
                          elementType: IStubElementType<*, *>,
                          val refName: String
) : StubBase<ElmExposedValue>(parent, elementType) {

    object Type : ElmStubElementType<ElmExposedValueStub, ElmExposedValue>("EXPOSED_VALUE") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmExposedValueStub, dataStream: StubOutputStream) {
            with(dataStream) {
                writeName(stub.refName)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmExposedValueStub(parentStub, this,
                        dataStream.readNameAsString()!!)

        override fun createPsi(stub: ElmExposedValueStub) =
                ElmExposedValue(stub, this)

        override fun createStub(psi: ElmExposedValue, parentStub: StubElement<*>?) =
                ElmExposedValueStub(parentStub, this, psi.referenceName)

        override fun indexStub(stub: ElmExposedValueStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmExposedOperatorStub(parent: StubElement<*>?,
                             elementType: IStubElementType<*, *>,
                             val refName: String
) : StubBase<ElmExposedOperator>(parent, elementType) {

    object Type : ElmStubElementType<ElmExposedOperatorStub, ElmExposedOperator>("EXPOSED_OPERATOR") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmExposedOperatorStub, dataStream: StubOutputStream) {
            with(dataStream) {
                writeName(stub.refName)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmExposedOperatorStub(parentStub, this,
                        dataStream.readNameAsString()!!)

        override fun createPsi(stub: ElmExposedOperatorStub) =
                ElmExposedOperator(stub, this)

        override fun createStub(psi: ElmExposedOperator, parentStub: StubElement<*>?) =
                ElmExposedOperatorStub(parentStub, this, psi.referenceName)

        override fun indexStub(stub: ElmExposedOperatorStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmExposedTypeStub(parent: StubElement<*>?,
                         elementType: IStubElementType<*, *>,
                         val refName: String,
                         val exposesAll: Boolean
) : StubBase<ElmExposedType>(parent, elementType) {

    object Type : ElmStubElementType<ElmExposedTypeStub, ElmExposedType>("EXPOSED_TYPE") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmExposedTypeStub, dataStream: StubOutputStream) {
            with(dataStream) {
                writeName(stub.refName)
                writeBoolean(stub.exposesAll)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmExposedTypeStub(parentStub, this,
                        dataStream.readNameAsString()!!,
                        dataStream.readBoolean())

        override fun createPsi(stub: ElmExposedTypeStub) =
                ElmExposedType(stub, this)

        override fun createStub(psi: ElmExposedType, parentStub: StubElement<*>?) =
                ElmExposedTypeStub(parentStub, this, psi.referenceName, psi.exposesAll)

        override fun indexStub(stub: ElmExposedTypeStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmExposedUnionConstructorStub(parent: StubElement<*>?,
                                     elementType: IStubElementType<*, *>,
                                     val refName: String
) : StubBase<ElmExposedUnionConstructor>(parent, elementType) {

    object Type : ElmStubElementType<ElmExposedUnionConstructorStub, ElmExposedUnionConstructor>("EXPOSED_UNION_CONSTRUCTOR") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmExposedUnionConstructorStub, dataStream: StubOutputStream) {
            with(dataStream) {
                writeName(stub.refName)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmExposedUnionConstructorStub(parentStub, this,
                        dataStream.readNameAsString()!!)

        override fun createPsi(stub: ElmExposedUnionConstructorStub) =
                ElmExposedUnionConstructor(stub, this)

        override fun createStub(psi: ElmExposedUnionConstructor, parentStub: StubElement<*>?) =
                ElmExposedUnionConstructorStub(parentStub, this, psi.referenceName)

        override fun indexStub(stub: ElmExposedUnionConstructorStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmExposedUnionConstructorsStub(parent: StubElement<*>?,
                                      elementType: IStubElementType<*, *>
) : StubBase<ElmExposedUnionConstructors>(parent, elementType) {

    object Type : ElmStubElementType<ElmExposedUnionConstructorsStub, ElmExposedUnionConstructors>("EXPOSED_UNION_CONSTRUCTORS") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmExposedUnionConstructorsStub, dataStream: StubOutputStream) {
            // nothing to write
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmExposedUnionConstructorsStub(parentStub, this)

        override fun createPsi(stub: ElmExposedUnionConstructorsStub) =
                ElmExposedUnionConstructors(stub, this)

        override fun createStub(psi: ElmExposedUnionConstructors, parentStub: StubElement<*>?) =
                ElmExposedUnionConstructorsStub(parentStub, this)

        override fun indexStub(stub: ElmExposedUnionConstructorsStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmValueDeclarationStub(parent: StubElement<*>?,
                              elementType: IStubElementType<*, *>
) : StubBase<ElmValueDeclaration>(parent, elementType) {

    object Type : ElmStubElementType<ElmValueDeclarationStub, ElmValueDeclaration>("VALUE_DECLARATION") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmValueDeclarationStub, dataStream: StubOutputStream) {
            // nothing to write
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmValueDeclarationStub(parentStub, this)

        override fun createPsi(stub: ElmValueDeclarationStub) =
                ElmValueDeclaration(stub, this)

        override fun createStub(psi: ElmValueDeclaration, parentStub: StubElement<*>?) =
                ElmValueDeclarationStub(parentStub, this)

        override fun indexStub(stub: ElmValueDeclarationStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmPortAnnotationStub(parent: StubElement<*>?,
                            elementType: IStubElementType<*, *>,
                            override val name: String
) : StubBase<ElmPortAnnotation>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmPortAnnotationStub, ElmPortAnnotation>("PORT_ANNOTATION") {

        override fun serialize(stub: ElmPortAnnotationStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmPortAnnotationStub(parentStub, this,
                        dataStream.readNameAsString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmPortAnnotationStub) =
                ElmPortAnnotation(stub, this)

        override fun createStub(psi: ElmPortAnnotation, parentStub: StubElement<*>?) =
                ElmPortAnnotationStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmPortAnnotationStub, sink: IndexSink) {
            sink.indexPortAnnotation(stub)
        }
    }
}


private fun StubInputStream.readNameAsString(): String? = readName()?.string
private fun StubInputStream.readUTFFastAsNullable(): String? = readUTFFast().let { if (it == "") null else it }

private fun StubOutputStream.writeUTFFastAsNullable(value: String?) = writeUTFFast(value ?: "")