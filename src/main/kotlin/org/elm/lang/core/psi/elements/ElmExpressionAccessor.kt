package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmPsiElementImpl

/**
 * An accessor on a parenthesized expression or record.
 *
 * e.g. `.foo.bar` in `(fn arg).foo.bar` and `{foo={bar=1}}.foo.bar`
 */
class ElmExpressionAccessor(node: ASTNode) : ElmPsiElementImpl(node)
