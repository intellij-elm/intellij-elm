package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


/**
 * An Elm expression wrapped in parentheses
 *
 * e.g. `(42)`
 */
class ElmParenthesizedExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag, ElmFunctionCallTargetTag, ElmFieldAccessTargetTag {

    /** In a well-formed program, this will never be null. */
    val expression: ElmExpressionTag? get() = findChildByClass(ElmExpressionTag::class.java)
}
