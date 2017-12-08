
package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmStubbedNamedElementImpl
import org.elm.lang.core.psi.ElmTypes.LOWER_CASE_IDENTIFIER
import org.elm.lang.core.psi.IdentifierCase
import org.elm.lang.core.stubs.ElmPortAnnotationStub


/**
 * A port annotation
 *
 * e.g. `port doSomething : String -> Int`
 *
 */
class ElmPortAnnotation : ElmStubbedNamedElementImpl<ElmPortAnnotationStub> {

    constructor(node: ASTNode) :
            super(node, IdentifierCase.LOWER)

    constructor(stub: ElmPortAnnotationStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType, IdentifierCase.LOWER)


    /**
     * The left-hand side of the type annotation which names the port
     *
     * e.g. `doSomething` in `port doSomething : String -> Int`
     */
    val lowerCaseIdentifier: PsiElement
        get() = findNotNullChildByType(LOWER_CASE_IDENTIFIER)


    /**
     * The right-hand side of the port annotation which give the port's type signature
     *
     * e.g. `String -> Int` in `port doSomething : String -> Int`
     */
    val typeRef: ElmTypeRef
        get() = findNotNullChildByClass(ElmTypeRef::class.java)

}
