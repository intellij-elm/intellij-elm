package org.elm.lang.core.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.util.io.DataInputOutputUtil.readNullable
import com.intellij.util.io.DataInputOutputUtil.writeNullable
import org.elm.lang.core.ElmLanguage
import org.elm.lang.core.psi.ElmFile
import org.elm.lang.core.psi.elements.*


class ElmFileStub(file: ElmFile?) : PsiFileStubImpl<ElmFile>(file) {

    override fun getType() = Type


    object Type : IStubFileElementType<ElmFileStub>(ElmLanguage) {

        override fun getStubVersion() = 24

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
    "UNION_VARIANT" -> ElmUnionVariantStub.Type
    "FUNCTION_DECLARATION_LEFT" -> ElmFunctionDeclarationLeftStub.Type
    "OPERATOR_DECLARATION_LEFT" -> ElmOperatorDeclarationLeftStub.Type  // TODO [drop 0.18] remove this line
    "INFIX_DECLARATION" -> ElmInfixDeclarationStub.Type
    "INFIX_FUNC_REF" -> ElmInfixFuncRefStub.Type
    "EXPOSING_LIST" -> ElmExposingListStub.Type
    "EXPOSED_OPERATOR" -> ElmExposedOperatorStub.Type
    "EXPOSED_VALUE" -> ElmExposedValueStub.Type
    "EXPOSED_TYPE" -> ElmExposedTypeStub.Type
    "EXPOSED_UNION_CONSTRUCTOR" -> ElmExposedUnionConstructorStub.Type
    "EXPOSED_UNION_CONSTRUCTORS" -> ElmPlaceholderStub.Type("EXPOSED_UNION_CONSTRUCTORS", ::ElmExposedUnionConstructors)
    "VALUE_DECLARATION" -> ElmPlaceholderStub.Type("VALUE_DECLARATION", ::ElmValueDeclaration)
    "PORT_ANNOTATION" -> ElmPortAnnotationStub.Type
    "TYPE_EXPRESSION" -> ElmPlaceholderStub.Type("TYPE_EXPRESSION", ::ElmTypeExpression)
    "RECORD_TYPE" -> ElmPlaceholderStub.Type("RECORD_TYPE", ::ElmRecordType)
    "FIELD_TYPE" -> ElmFieldTypeStub.Type
    "TUPLE_TYPE" -> ElmPlaceholderStub.Type("TUPLE_TYPE", ::ElmTupleType)
    "UNIT_EXPR" -> ElmPlaceholderStub.Type("UNIT_EXPR", ::ElmUnitExpr)
    "TYPE_REF" -> ElmPlaceholderStub.Type("TYPE_REF", ::ElmTypeRef)
    "TYPE_VARIABLE" -> ElmTypeVariableStub.Type
    "LOWER_TYPE_NAME" -> ElmLowerTypeNameStub.Type
    "RECORD_BASE_IDENTIFIER" -> ElmRecordBaseIdentifierStub.Type
    "TYPE_ANNOTATION" -> ElmTypeAnnotationStub.Type
    "IMPORT_CLAUSE" -> ElmPlaceholderStub.Type("IMPORT_CLAUSE", ::ElmImportClause)
    "AS_CLAUSE" -> ElmAsClauseStub.Type
    "UPPER_CASE_QID" -> ElmUpperCaseQIDStub.Type
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
                        dataStream.readNameString() ?: error("expected non-null string"),
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
                        dataStream.readNameString() ?: error("expected non-null string"))

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
                        dataStream.readNameString() ?: error("expected non-null string"),
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


class ElmUnionVariantStub(parent: StubElement<*>?,
                          elementType: IStubElementType<*, *>,
                          override val name: String
) : StubBase<ElmUnionVariant>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmUnionVariantStub, ElmUnionVariant>("UNION_VARIANT") {

        override fun serialize(stub: ElmUnionVariantStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmUnionVariantStub(parentStub, this,
                        dataStream.readNameString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmUnionVariantStub) =
                ElmUnionVariant(stub, this)

        override fun createStub(psi: ElmUnionVariant, parentStub: StubElement<*>?) =
                ElmUnionVariantStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmUnionVariantStub, sink: IndexSink) {
            sink.indexUnionVariant(stub)
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
                        dataStream.readNameString() ?: error("expected non-null string"))

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
                        dataStream.readNameString() ?: error("expected non-null string"))

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
                              override val name: String,
                              val precedence: Int,
                              val associativity: String,
                              val funcRefName: String?
) : StubBase<ElmInfixDeclaration>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmInfixDeclarationStub, ElmInfixDeclaration>("INFIX_DECLARATION") {

        override fun serialize(stub: ElmInfixDeclarationStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeUTFFast(stub.name)
                    writeInt(stub.precedence)
                    writeUTFFast(stub.associativity)
                    writeUTFFastAsNullable(stub.funcRefName)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmInfixDeclarationStub(parentStub, this,
                        dataStream.readUTFFast(),
                        dataStream.readInt(),
                        dataStream.readUTFFast(),
                        dataStream.readUTFFastAsNullable()
                )

        override fun createPsi(stub: ElmInfixDeclarationStub) =
                ElmInfixDeclaration(stub, this)

        override fun createStub(psi: ElmInfixDeclaration, parentStub: StubElement<*>?) =
                ElmInfixDeclarationStub(
                        parentStub,
                        this,
                        psi.name,
                        psi.precedence ?: 0,
                        psi.associativityElement.text,
                        psi.funcRef?.referenceName
                )

        override fun indexStub(stub: ElmInfixDeclarationStub, sink: IndexSink) {
            sink.indexInfixDecl(stub)
        }
    }
}

class ElmInfixFuncRefStub(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>,
        val refName: String
) : StubBase<ElmInfixFuncRef>(parent, elementType) {

    object Type : ElmStubElementType<ElmInfixFuncRefStub, ElmInfixFuncRef>("INFIX_FUNC_REF") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmInfixFuncRefStub, dataStream: StubOutputStream) {
            with(dataStream) {
                writeName(stub.refName)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmInfixFuncRefStub(parentStub, this,
                        dataStream.readNameString()!!)

        override fun createPsi(stub: ElmInfixFuncRefStub) =
                ElmInfixFuncRef(stub, this)

        override fun createStub(psi: ElmInfixFuncRef, parentStub: StubElement<*>?) =
                ElmInfixFuncRefStub(parentStub, this, psi.referenceName)

        override fun indexStub(stub: ElmInfixFuncRefStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmExposingListStub(
        parent: StubElement<*>?,
                          elementType: IStubElementType<*, *>,
                          val exposesAll: Boolean
) : StubBase<ElmExposingList>(parent, elementType) {

    object Type : ElmStubElementType<ElmExposingListStub, ElmExposingList>("EXPOSING_LIST") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmExposingListStub, dataStream: StubOutputStream) {
            with(dataStream) {
                writeBoolean(stub.exposesAll)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmExposingListStub(parentStub, this,
                        dataStream.readBoolean())

        override fun createPsi(stub: ElmExposingListStub) =
                ElmExposingList(stub, this)

        override fun createStub(psi: ElmExposingList, parentStub: StubElement<*>?) =
                ElmExposingListStub(parentStub, this, psi.exposesAll)

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
                        dataStream.readNameString()!!)

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
                        dataStream.readNameString()!!)

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
                        dataStream.readNameString()!!,
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
                        dataStream.readNameString()!!)

        override fun createPsi(stub: ElmExposedUnionConstructorStub) =
                ElmExposedUnionConstructor(stub, this)

        override fun createStub(psi: ElmExposedUnionConstructor, parentStub: StubElement<*>?) =
                ElmExposedUnionConstructorStub(parentStub, this, psi.referenceName)

        override fun indexStub(stub: ElmExposedUnionConstructorStub, sink: IndexSink) {
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
                        dataStream.readNameString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmPortAnnotationStub) =
                ElmPortAnnotation(stub, this)

        override fun createStub(psi: ElmPortAnnotation, parentStub: StubElement<*>?) =
                ElmPortAnnotationStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmPortAnnotationStub, sink: IndexSink) {
            sink.indexPortAnnotation(stub)
        }
    }
}

class ElmFieldTypeStub(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>,
        override val name: String
) : StubBase<ElmFieldType>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmFieldTypeStub, ElmFieldType>("FIELD_TYPE") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmFieldTypeStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmFieldTypeStub(parentStub, this,
                        dataStream.readNameString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmFieldTypeStub) =
                ElmFieldType(stub, this)

        override fun createStub(psi: ElmFieldType, parentStub: StubElement<*>?) =
                ElmFieldTypeStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmFieldTypeStub, sink: IndexSink) {
        }
    }
}

class ElmUpperCaseQIDStub(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>,
        val refName: String,
        val qualifierPrefix: String
) : StubBase<ElmUpperCaseQID>(parent, elementType) {

    object Type : ElmStubElementType<ElmUpperCaseQIDStub, ElmUpperCaseQID>("UPPER_CASE_QID") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmUpperCaseQIDStub, dataStream: StubOutputStream) {
            with(dataStream) {
                writeName(stub.refName)
                writeName(stub.qualifierPrefix)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): ElmUpperCaseQIDStub {
            return ElmUpperCaseQIDStub(parentStub, this,
                    dataStream.readNameString() ?: error("refName: expected non-null string"),
                    dataStream.readNameString() ?: error("qualifierPrefix: expected non-null string")
            )
        }

        override fun createPsi(stub: ElmUpperCaseQIDStub) =
                ElmUpperCaseQID(stub, this)

        override fun createStub(psi: ElmUpperCaseQID, parentStub: StubElement<*>?) =
                ElmUpperCaseQIDStub(parentStub, this, psi.refName, psi.qualifierPrefix)

        override fun indexStub(stub: ElmUpperCaseQIDStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmTypeVariableStub(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>,
        override val name: String
) : StubBase<ElmTypeVariable>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmTypeVariableStub, ElmTypeVariable>("TYPE_VARIABLE") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmTypeVariableStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmTypeVariableStub(parentStub, this,
                        dataStream.readNameString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmTypeVariableStub) =
                ElmTypeVariable(stub, this)

        override fun createStub(psi: ElmTypeVariable, parentStub: StubElement<*>?) =
                ElmTypeVariableStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmTypeVariableStub, sink: IndexSink) {
        }
    }
}

class ElmLowerTypeNameStub(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>,
        override val name: String
) : StubBase<ElmLowerTypeName>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmLowerTypeNameStub, ElmLowerTypeName>("LOWER_TYPE_NAME") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmLowerTypeNameStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmLowerTypeNameStub(parentStub, this,
                        dataStream.readNameString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmLowerTypeNameStub) =
                ElmLowerTypeName(stub, this)

        override fun createStub(psi: ElmLowerTypeName, parentStub: StubElement<*>?) =
                ElmLowerTypeNameStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmLowerTypeNameStub, sink: IndexSink) {
        }
    }
}

class ElmRecordBaseIdentifierStub(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>,
        val refName: String
) : StubBase<ElmRecordBaseIdentifier>(parent, elementType) {

    object Type : ElmStubElementType<ElmRecordBaseIdentifierStub, ElmRecordBaseIdentifier>("RECORD_BASE_IDENTIFIER") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmRecordBaseIdentifierStub, dataStream: StubOutputStream) {
            with(dataStream) {
                writeName(stub.refName)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmRecordBaseIdentifierStub(parentStub, this,
                        dataStream.readNameString()!!)

        override fun createPsi(stub: ElmRecordBaseIdentifierStub) =
                ElmRecordBaseIdentifier(stub, this)

        override fun createStub(psi: ElmRecordBaseIdentifier, parentStub: StubElement<*>?) =
                ElmRecordBaseIdentifierStub(parentStub, this, psi.referenceName)

        override fun indexStub(stub: ElmRecordBaseIdentifierStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmTypeAnnotationStub(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>,
        val refName: String
) : StubBase<ElmTypeAnnotation>(parent, elementType) {

    object Type : ElmStubElementType<ElmTypeAnnotationStub, ElmTypeAnnotation>("TYPE_ANNOTATION") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmTypeAnnotationStub, dataStream: StubOutputStream) {
            with(dataStream) {
                writeName(stub.refName)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmTypeAnnotationStub(parentStub, this,
                        dataStream.readNameString()!!)

        override fun createPsi(stub: ElmTypeAnnotationStub) =
                ElmTypeAnnotation(stub, this)

        override fun createStub(psi: ElmTypeAnnotation, parentStub: StubElement<*>?) =
                ElmTypeAnnotationStub(parentStub, this, psi.referenceName)

        override fun indexStub(stub: ElmTypeAnnotationStub, sink: IndexSink) {
            // no-op
        }
    }
}

class ElmAsClauseStub(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>,
        override val name: String
) : StubBase<ElmAsClause>(parent, elementType), ElmNamedStub {

    object Type : ElmStubElementType<ElmAsClauseStub, ElmAsClause>("AS_CLAUSE") {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmAsClauseStub, dataStream: StubOutputStream) =
                with(dataStream) {
                    writeName(stub.name)
                }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmAsClauseStub(parentStub, this,
                        dataStream.readNameString() ?: error("expected non-null string"))

        override fun createPsi(stub: ElmAsClauseStub) =
                ElmAsClause(stub, this)

        override fun createStub(psi: ElmAsClause, parentStub: StubElement<*>?) =
                ElmAsClauseStub(parentStub, this, psi.name)

        override fun indexStub(stub: ElmAsClauseStub, sink: IndexSink) {
        }
    }
}


private fun StubInputStream.readUTFFastAsNullable(): String? = readNullable(this, this::readUTFFast)
private fun StubOutputStream.writeUTFFastAsNullable(value: String?) = writeNullable(this, value, this::writeUTFFast)

private fun <E : Enum<E>> StubOutputStream.writeEnum(e: E) = writeByte(e.ordinal)
private inline fun <reified E : Enum<E>> StubInputStream.readEnum(): E = enumValues<E>()[readUnsignedByte()]

private fun StubOutputStream.writeLongAsNullable(value: Long?) = writeNullable(this, value, this::writeLong)
private fun StubInputStream.readLongAsNullable(): Long? = readNullable(this, this::readLong)

private fun StubOutputStream.writeDoubleAsNullable(value: Double?) = writeNullable(this, value, this::writeDouble)
private fun StubInputStream.readDoubleAsNullable(): Double? = readNullable(this, this::readDouble)
