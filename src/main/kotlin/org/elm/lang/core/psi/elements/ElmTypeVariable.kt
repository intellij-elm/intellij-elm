package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import org.elm.lang.core.psi.*
import org.elm.lang.core.resolve.ElmReferenceElement
import org.elm.lang.core.resolve.reference.TypeVariableReference

/**
 * Holds a lower-case identifier within a type expression which
 * gives the type variable in a parametric type.
 *
 * e.g. the 'a' in `map : (a -> b) -> List a -> List b`
 * e.g. the last `a` in `type Foo a = Bar a`
 */
class ElmTypeVariable(node: ASTNode) :
        ElmNamedElementImpl(node, IdentifierCase.LOWER),
        ElmReferenceElement,
        ElmUnionVariantParameterTag,
        ElmTypeRefArgumentTag,
        ElmTypeExpressionSegmentTag {

    val identifier: PsiElement
        get() = findNotNullChildByType(ElmTypes.LOWER_CASE_IDENTIFIER)

    override val referenceNameElement: PsiElement
        get() = identifier

    override val referenceName: String
        get() = referenceNameElement.text

    override fun getReference() = TypeVariableReference(this)

    // type variables can only be referenced within the declaration they're declared in, and, in the
    // case of function annotations, from annotations of nested functions
    override fun getUseScope(): SearchScope {
        return LocalSearchScope(elmFile)
    }
}
