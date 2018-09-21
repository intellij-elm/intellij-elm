package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmFunctionCallTarget
import org.elm.lang.core.psi.ElmPsiElementImpl

/**
 * A function expression that will access a field in a record.
 *
 * e.g. `.x` in `List.map .x [{x=1}]`
 */
class ElmFieldAccessorFunction(node: ASTNode) : ElmPsiElementImpl(node), ElmFunctionCallTarget
