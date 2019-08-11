package org.elm.lang.core.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.*
import org.elm.lang.core.psi.ElmPsiElement
import org.elm.lang.core.resolve.ElmReferenceElement


/**
 * The placeholder stub is used for any stub element which does not need to store additional data.
 * It's only purpose is to decrease boilerplate for elements whose only reason they exist as a
 * stub is to hold stub children.
 *
 * @see ElmPlaceholderRefStub
 */
class ElmPlaceholderStub(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>
) : StubBase<ElmPsiElement>(parent, elementType) {

    class Type<T : ElmPsiElement>(
            name: String,
            private val ctor: (ElmPlaceholderStub, IStubElementType<*, *>) -> T
    ) : ElmStubElementType<ElmPlaceholderStub, T>(name) {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmPlaceholderStub, dataStream: StubOutputStream) {
            // nothing extra to write
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmPlaceholderStub(parentStub, this) // nothing extra to read

        override fun createPsi(stub: ElmPlaceholderStub) =
                ctor(stub, this)

        override fun createStub(psi: T, parentStub: StubElement<*>?) =
                ElmPlaceholderStub(parentStub, this)

        override fun indexStub(stub: ElmPlaceholderStub, sink: IndexSink) {
            // no-op
        }
    }
}


/**
 * Like [ElmPlaceholderStub] but for the common-case where the stub needs to also
 * hold its reference name.
 *
 * @see ElmPlaceholderStub
 */
class ElmPlaceholderRefStub(
        parent: StubElement<*>?,
        elementType: IStubElementType<*, *>,
        val refName: String
) : StubBase<ElmReferenceElement>(parent, elementType) {

    class Type<T : ElmReferenceElement>(
            name: String,
            private val ctor: (ElmPlaceholderRefStub, IStubElementType<*, *>) -> T
    ) : ElmStubElementType<ElmPlaceholderRefStub, T>(name) {

        override fun shouldCreateStub(node: ASTNode) =
                createStubIfParentIsStub(node)

        override fun serialize(stub: ElmPlaceholderRefStub, dataStream: StubOutputStream) {
            with(dataStream) {
                writeName(stub.refName)
            }
        }

        override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
                ElmPlaceholderRefStub(parentStub, this,
                        dataStream.readNameString()!!)

        override fun createPsi(stub: ElmPlaceholderRefStub) =
                ctor(stub, this)

        override fun createStub(psi: T, parentStub: StubElement<*>?) =
                ElmPlaceholderRefStub(parentStub, this, psi.referenceName)

        override fun indexStub(stub: ElmPlaceholderRefStub, sink: IndexSink) {
            // no-op
        }
    }
}