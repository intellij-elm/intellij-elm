package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


/**
 * A record literal with an accessor.
 *
 * e.g. `{foo={bar=1}}.foo.bar`
 */
class ElmRecordWithAccessor(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag, ElmFunctionCallTarget {
    val record: ElmRecord get() = childOfType()!!
    val accessor: ElmExpressionAccessor get() = childOfType()!!
}
