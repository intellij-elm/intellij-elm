package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.ElmConstantTag
import org.elm.lang.core.psi.ElmPsiElementImpl


/** A literal number. e.g. `-123` or `1.23` */
class ElmNumberConstantExpr(node: ASTNode) : ElmPsiElementImpl(node), ElmConstantTag {
    val isFloat get() = text.contains('.')
}
