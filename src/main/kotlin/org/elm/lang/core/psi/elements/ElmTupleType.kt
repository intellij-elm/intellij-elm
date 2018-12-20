package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.elm.lang.core.psi.ElmTypeRefArgumentTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.ElmTypeExpressionSegmentTag
import org.elm.lang.core.psi.ElmUnionVariantParameterTag


/**
 * A type expression for a tuple
 *
 * e.g. `(Int, String)` in a type declaration or annotation
 */
class ElmTupleType(node: ASTNode) : ElmPsiElementImpl(node), ElmUnionVariantParameterTag, ElmTypeRefArgumentTag, ElmTypeExpressionSegmentTag {

    val typeExpressionList: List<ElmTypeExpression>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, ElmTypeExpression::class.java)

    val unitExpr: ElmUnitExpr?
        get() = findChildByClass(ElmUnitExpr::class.java)

}
