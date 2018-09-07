package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


/** A literal number. e.g. `-123` or `1.23` */
class ElmNumberConstant(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag, ElmConsPatternChildTag,
        ElmFunctionParamTag, ElmPatternChildTag, ElmConstantTag {
    val isFloat get() = text.contains('.')
}
