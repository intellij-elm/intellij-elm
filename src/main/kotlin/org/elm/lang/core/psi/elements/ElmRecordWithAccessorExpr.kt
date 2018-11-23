package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmFunctionCallTargetTag
import org.elm.lang.core.psi.ElmAtomTag
import org.elm.lang.core.psi.ElmPsiElementImpl
import org.elm.lang.core.psi.childOfType


/**
 * A record literal with an accessor.
 *
 * e.g. `{foo={bar=1}}.foo.bar`
 */
class ElmRecordWithAccessorExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmAtomTag, ElmFunctionCallTargetTag {
    val record: ElmRecordExpr get() = childOfType()!!
    val accessor: ElmExpressionAccessor get() = childOfType()!!
}
