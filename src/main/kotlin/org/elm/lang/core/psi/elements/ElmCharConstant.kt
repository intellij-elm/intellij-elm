package org.elm.lang.core.psi.elements

import com.intellij.lang.ASTNode
import org.elm.lang.core.psi.*


/** A literal char. e.g. `'x'` or `'\u{0042}'` */
class ElmCharConstant(node: ASTNode) : ElmPsiElementImpl(node), ElmOperandTag, ElmConsPatternChildTag,
        ElmFunctionParamTag, ElmPatternChildTag, ElmConstantTag
