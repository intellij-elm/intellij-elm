package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.elm.lang.core.psi.ElmQID
import org.elm.lang.core.psi.ElmStubbedElement
import org.elm.lang.core.psi.ElmTypes
import org.elm.lang.core.psi.ElmTypes.UPPER_CASE_IDENTIFIER
import org.elm.lang.core.stubs.ElmUpperCaseQIDStub
import kotlin.math.max

/**
 * An identifier that refers to a Module, Union Constructor, or Record Constructor,
 * and it may contain an additional qualifier prefix which identifies the module/alias
 * from which the identifier may be obtained.
 */
class ElmUpperCaseQID : ElmStubbedElement<ElmUpperCaseQIDStub>, ElmQID {

    constructor(node: ASTNode) :
            super(node)

    constructor(stub: ElmUpperCaseQIDStub, stubType: IStubElementType<*, *>) :
            super(stub, stubType)


    /**
     * Guaranteed to contain at least one element
     */
    override val upperCaseIdentifierList: List<PsiElement>
        get() = findChildrenByType(UPPER_CASE_IDENTIFIER)

    override val qualifiers: List<PsiElement>
        get() = upperCaseIdentifierList.dropLast(1)

    /**
     * The qualifier prefix, if any.
     *
     * e.g. `"Foo.Bar"` for QID `Foo.Bar.Quux`
     * e.g. `""` for QID `Foo`
     */
    override val qualifierPrefix: String
        get() = stub?.qualifierPrefix ?: text.let { it.take(max(0, it.lastIndexOf('.'))) }

    /**
     * The right-most name in a potentially qualified name.
     *
     * e.g. `"Quux"` for QID `Foo.Bar.Quux`
     * e.g. `"Foo"` for QID `Foo`
     */
    val refName: String
        get() = stub?.refName ?: text.let { it.drop(it.lastIndexOf(".") + 1) }

    /**
     * The fully qualified name. Equivalent to `PsiElement#text` but stub-safe.
     *
     * e.g. `"Foo.Bar.Quux"` for QID `Foo.Bar.Quux`
     */
    val fullName: String
        get() = when {
            stub == null -> text
            isQualified -> "$qualifierPrefix.$refName"
            else -> refName
        }

    /**
     * True if the identifier is qualified by a module name (in the case of union or
     * record constructors) or the module exists in a hierarchy (in the case of a pure
     * module name in a module decl or import decl).
     *
     * TODO [kl] this double-duty is a bit strange. Maybe make a separate Psi element?
     */
    override val isQualified: Boolean
        get() = stub?.let { it.qualifierPrefix != "" } ?: (findChildByType<PsiElement>(ElmTypes.DOT) != null)
}
